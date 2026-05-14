package com.animalplatform.payment.application;

import com.animalplatform.animal.domain.Animal;
import com.animalplatform.animal.domain.AnimalRepository;
import com.animalplatform.auth.application.AuthenticatedUserService;
import com.animalplatform.donation.domain.Donation;
import com.animalplatform.donation.domain.DonationRepository;
import com.animalplatform.donation.domain.DonationStatus;
import com.animalplatform.donation.domain.DonationTargetType;
import com.animalplatform.donation.domain.DonationType;
import com.animalplatform.payment.application.PortOneClient.PortOnePayment;
import com.animalplatform.payment.domain.PreparedPayment;
import com.animalplatform.payment.domain.PreparedPaymentRepository;
import com.animalplatform.payment.presentation.PaymentController.ConfirmPaymentRequest;
import com.animalplatform.payment.presentation.PaymentController.PreparePaymentRequest;
import com.animalplatform.shelter.domain.Shelter;
import com.animalplatform.shelter.domain.ShelterRepository;
import com.animalplatform.user.application.DonorTierService;
import com.animalplatform.user.domain.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final AuthenticatedUserService authenticatedUserService;
    private final DonationRepository donationRepository;
    private final ShelterRepository shelterRepository;
    private final AnimalRepository animalRepository;
    private final PortOneClient portOneClient;
    private final DonorTierService donorTierService;
    private final PreparedPaymentRepository preparedPaymentRepository;
    private final long preparedPaymentTtlMinutes;

    public PaymentService(
            AuthenticatedUserService authenticatedUserService,
            DonationRepository donationRepository,
            ShelterRepository shelterRepository,
            AnimalRepository animalRepository,
            PortOneClient portOneClient,
            DonorTierService donorTierService,
            PreparedPaymentRepository preparedPaymentRepository,
            @Value("${app.payment.prepare-ttl-minutes:30}") long preparedPaymentTtlMinutes
    ) {
        this.authenticatedUserService = authenticatedUserService;
        this.donationRepository = donationRepository;
        this.shelterRepository = shelterRepository;
        this.animalRepository = animalRepository;
        this.portOneClient = portOneClient;
        this.donorTierService = donorTierService;
        this.preparedPaymentRepository = preparedPaymentRepository;
        this.preparedPaymentTtlMinutes = preparedPaymentTtlMinutes;
    }

    @Transactional
    public Map<String, Object> preparePayment(PreparePaymentRequest request, String merchantUid) {
        User user = authenticatedUserService.getCurrentUser();
        DonationTargetType targetType = parseTargetType(request.targetType());
        DonationType donationType = parseDonationType(request.donationType());
        LocalDateTime now = LocalDateTime.now();

        if (targetType == DonationTargetType.SHELTER) {
            shelterRepository.findById(request.targetId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "보호소 정보를 찾을 수 없습니다."));
        } else {
            animalRepository.findById(request.targetId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "동물 정보를 찾을 수 없습니다."));
        }

        preparedPaymentRepository.deleteByExpiresAtBefore(now);
        preparedPaymentRepository.save(new PreparedPayment(
                merchantUid,
                user.getId(),
                targetType,
                donationType,
                request.targetId(),
                request.amount(),
                request.orderName(),
                request.buyerName(),
                request.buyerEmail(),
                request.buyerTel(),
                request.subscriptionInterval(),
                now.plusMinutes(preparedPaymentTtlMinutes)
        ));

        return Map.ofEntries(
                Map.entry("merchantUid", merchantUid),
                Map.entry("targetType", targetType.name()),
                Map.entry("donationType", donationType.name()),
                Map.entry("targetId", request.targetId()),
                Map.entry("orderName", request.orderName()),
                Map.entry("amount", request.amount()),
                Map.entry("buyerName", request.buyerName()),
                Map.entry("buyerEmail", request.buyerEmail()),
                Map.entry("buyerTel", request.buyerTel()),
                Map.entry("subscriptionInterval", request.subscriptionInterval() == null ? "" : request.subscriptionInterval()),
                Map.entry("status", "READY")
        );
    }

    @Transactional
    public Map<String, Object> confirmPayment(ConfirmPaymentRequest request) {
        PreparedPayment preparedPayment = preparedPaymentRepository.findByMerchantUid(request.merchantUid())
                .orElse(null);

        if (preparedPayment == null) {
            log.warn("Payment verification rejected. reason=prepared_payment_missing, impUid={}, merchantUid={}",
                    request.impUid(), request.merchantUid());
            throw new ResponseStatusException(BAD_REQUEST, "준비된 결제 정보가 없습니다.");
        }
        if (preparedPayment.isExpired(LocalDateTime.now())) {
            preparedPaymentRepository.delete(preparedPayment);
            throw new ResponseStatusException(BAD_REQUEST, "결제 준비 시간이 만료되었습니다.");
        }

        User user = authenticatedUserService.getCurrentUser();
        if (!user.getId().equals(preparedPayment.getUserId())) {
            log.warn("Payment verification rejected. reason=user_mismatch, impUid={}, merchantUid={}, userId={}",
                    request.impUid(), request.merchantUid(), user.getId());
            throw new ResponseStatusException(BAD_REQUEST, "결제 사용자 정보가 일치하지 않습니다.");
        }

        if (preparedPayment.getDonationType() == DonationType.SUBSCRIPTION) {
            throw new ResponseStatusException(BAD_REQUEST, "정기 후원은 빌링키 연동 후 결제 확정이 가능합니다.");
        }

        PortOnePayment actualPayment = portOneClient.getPayment(request.impUid());
        verifyPortOnePayment(request, preparedPayment, actualPayment);

        Shelter shelter = null;
        Animal animal = null;
        if (preparedPayment.getTargetType() == DonationTargetType.SHELTER) {
            shelter = shelterRepository.findById(preparedPayment.getTargetId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "보호소 정보를 찾을 수 없습니다."));
        } else {
            animal = animalRepository.findById(preparedPayment.getTargetId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "동물 정보를 찾을 수 없습니다."));
        }

        long previousTotalAmount = donationRepository.sumPaidAmountByUserId(user.getId()).longValue();
        Donation donation = new Donation(
                user,
                shelter,
                animal,
                preparedPayment.getTargetType(),
                preparedPayment.getDonationType(),
                preparedPayment.getAmount(),
                DonationStatus.READY,
                null,
                null
        );
        donation.markPaid(request.impUid(), LocalDateTime.now());
        donationRepository.save(donation);
        donorTierService.checkAndNotifyTierChange(
                user,
                previousTotalAmount,
                previousTotalAmount + preparedPayment.getAmount().longValue()
        );
        preparedPaymentRepository.delete(preparedPayment);

        log.info("Payment verification succeeded. donationId={}, impUid={}, merchantUid={}, amount={}",
                donation.getId(), request.impUid(), request.merchantUid(), preparedPayment.getAmount());

        return Map.of(
                "donationId", donation.getId(),
                "impUid", request.impUid(),
                "merchantUid", request.merchantUid(),
                "paidAmount", actualPayment.amount(),
                "status", donation.getPaymentStatus().name(),
                "verified", true,
                "message", "PortOne 검증 완료 후 후원 내역이 저장되었습니다."
        );
    }

    private void verifyPortOnePayment(
            ConfirmPaymentRequest request,
            PreparedPayment preparedPayment,
            PortOnePayment actualPayment
    ) {
        boolean merchantUidMatches = request.merchantUid().equals(actualPayment.merchantUid());
        boolean amountMatches = preparedPayment.getAmount().compareTo(actualPayment.amount()) == 0;
        boolean paid = "paid".equalsIgnoreCase(actualPayment.status());

        log.info(
                "Payment verification attempt. impUid={}, merchantUid={}, expectedAmount={}, actualAmount={}, actualStatus={}",
                request.impUid(),
                request.merchantUid(),
                preparedPayment.getAmount(),
                actualPayment.amount(),
                actualPayment.status()
        );

        if (merchantUidMatches && amountMatches && paid) {
            return;
        }

        portOneClient.cancelPayment(
                request.impUid(),
                request.merchantUid(),
                "Payment verification failed."
        );
        preparedPaymentRepository.delete(preparedPayment);

        log.warn(
                "Payment verification failed. impUid={}, merchantUid={}, merchantUidMatches={}, amountMatches={}, paid={}",
                request.impUid(),
                request.merchantUid(),
                merchantUidMatches,
                amountMatches,
                paid
        );

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "결제 검증에 실패하여 결제를 취소했습니다."
        );
    }

    private DonationTargetType parseTargetType(String targetType) {
        try {
            return DonationTargetType.valueOf(targetType.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(BAD_REQUEST, "지원하지 않는 후원 대상입니다.");
        }
    }

    private DonationType parseDonationType(String donationType) {
        try {
            return DonationType.valueOf(donationType.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(BAD_REQUEST, "지원하지 않는 후원 방식입니다.");
        }
    }

}
