package com.animalplatform.payment.presentation;



import com.animalplatform.common.api.CommonApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.animalplatform.common.api.ApiResponse;
import com.animalplatform.payment.application.PaymentService;
import com.animalplatform.payment.config.PortOneProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@Tag(name = "후원/결제", description = "해당 도메인 API")
@CommonApiResponses
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PortOneProperties portOneProperties;
    private final PaymentService paymentService;

    public PaymentController(PortOneProperties portOneProperties, PaymentService paymentService) {
        this.portOneProperties = portOneProperties;
        this.paymentService = paymentService;
    }

    @GetMapping("/portone/config")
        @Operation(summary = "요청 처리", description = "해당 API 요청을 처리합니다.")
    public ApiResponse<Map<String, Object>> getPortOneConfig() {
        return ApiResponse.ok(Map.of(
                "enabled", StringUtils.hasText(portOneProperties.getStoreId()),
                "storeId", defaultIfBlank(portOneProperties.getStoreId()),
                "channelKey", defaultIfBlank(portOneProperties.getChannelKey())
        ));
    }

    @PostMapping("/prepare")
        @Operation(summary = "요청 처리", description = "해당 API 요청을 처리합니다.")
    public ApiResponse<Map<String, Object>> preparePayment(@Valid @RequestBody PreparePaymentRequest request) {
        String merchantUid = createMerchantUid();
        return ApiResponse.ok(paymentService.preparePayment(request, merchantUid));
    }

    @PostMapping("/confirm")
        @Operation(summary = "요청 처리", description = "해당 API 요청을 처리합니다.")
    public ApiResponse<Map<String, Object>> confirmPayment(@Valid @RequestBody ConfirmPaymentRequest request) {
        return ApiResponse.ok(paymentService.confirmPayment(request));
    }

    public record PreparePaymentRequest(
            @NotBlank String targetType,
            @NotBlank String donationType,
            @NotNull Long targetId,
            @NotBlank String orderName,
            @NotNull @DecimalMin("1000") BigDecimal amount,
            @NotBlank String buyerName,
            @NotBlank String buyerEmail,
            @NotBlank String buyerTel,
            String subscriptionInterval
    ) {
    }

    public record ConfirmPaymentRequest(
            @NotBlank String impUid,
            @NotBlank String merchantUid
    ) {
    }

    private String createMerchantUid() {
        return "donation-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String defaultIfBlank(String value) {
        return StringUtils.hasText(value) ? value : "";
    }
}
