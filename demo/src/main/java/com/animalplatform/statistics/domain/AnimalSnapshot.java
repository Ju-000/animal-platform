package com.animalplatform.statistics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(
        name = "animal_snapshots",
        indexes = {
                @Index(name = "idx_animal_snapshots_process_state", columnList = "processState"),
                @Index(name = "idx_animal_snapshots_sido_collected_at", columnList = "sido,collectedAt"),
                @Index(name = "idx_animal_snapshots_sex_cd", columnList = "sexCd"),
                @Index(name = "idx_animal_snapshots_happen_dt", columnList = "happenDt")
        }
)
public class AnimalSnapshot {

    @Id
    @Column(nullable = false, length = 80)
    private String desertionNo;

    @Column(length = 1000)
    private String filename;

    @Column(length = 20)
    private String happenDt;

    @Column(length = 500)
    private String happenPlace;

    @Column(length = 255)
    private String kindCd;

    @Column(length = 100)
    private String colorCd;

    @Column(length = 100)
    private String age;

    @Column(length = 100)
    private String weight;

    @Column(length = 20)
    private String noticeSdt;

    @Column(length = 20)
    private String noticeEdt;

    @Column(length = 1000)
    private String popfile;

    @Column(length = 100)
    private String processState;

    @Column(length = 20)
    private String sexCd;

    @Column(length = 20)
    private String neuterYn;

    @Column(columnDefinition = "TEXT")
    private String specialMark;

    @Column(length = 255)
    private String careNm;

    @Column(length = 500)
    private String careAddr;

    @Column(length = 50)
    private String careTel;

    @Column(length = 100)
    private String careRegNo;

    @Column(length = 255)
    private String orgNm;

    @Column(length = 100)
    private String chargeNm;

    @Column(length = 50)
    private String officetel;

    @Column(length = 100)
    private String noticeNo;

    @Column(length = 50)
    private String sido;

    @Column(nullable = false)
    private LocalDateTime collectedAt;

    protected AnimalSnapshot() {
    }

    public AnimalSnapshot(String desertionNo) {
        this.desertionNo = desertionNo;
    }

    public void updateFrom(Map<String, Object> source, LocalDateTime collectedAt) {
        this.filename = firstNonBlank(
                text(source, "filename"),
                text(source, "popfile"),
                text(source, "popfile1"),
                text(source, "popfile2"),
                text(source, "popfile3"),
                text(source, "popfile4"),
                text(source, "popfile5")
        );
        this.happenDt = text(source, "happenDt");
        this.happenPlace = text(source, "happenPlace");
        this.kindCd = text(source, "kindCd");
        this.colorCd = text(source, "colorCd");
        this.age = text(source, "age");
        this.weight = text(source, "weight");
        this.noticeSdt = text(source, "noticeSdt");
        this.noticeEdt = text(source, "noticeEdt");
        this.popfile = firstNonBlank(
                text(source, "popfile"),
                text(source, "popfile1"),
                text(source, "popfile2"),
                text(source, "popfile3"),
                text(source, "popfile4"),
                text(source, "popfile5"),
                this.filename
        );
        this.processState = text(source, "processState");
        this.sexCd = text(source, "sexCd");
        this.neuterYn = text(source, "neuterYn");
        this.specialMark = text(source, "specialMark");
        this.careNm = text(source, "careNm");
        this.careAddr = text(source, "careAddr");
        this.careTel = text(source, "careTel");
        this.careRegNo = text(source, "careRegNo");
        this.orgNm = text(source, "orgNm");
        this.chargeNm = text(source, "chargeNm");
        this.officetel = text(source, "officetel");
        this.noticeNo = text(source, "noticeNo");
        this.sido = normalizeSido(firstNonBlank(this.orgNm, this.careAddr, this.happenPlace));
        this.collectedAt = collectedAt;
    }

    public String getDesertionNo() {
        return desertionNo;
    }

    public String getHappenDt() {
        return happenDt;
    }

    public String getHappenPlace() {
        return happenPlace;
    }

    public String getProcessState() {
        return processState;
    }

    public String getKindCd() {
        return kindCd;
    }

    public String getColorCd() {
        return colorCd;
    }

    public String getAge() {
        return age;
    }

    public String getWeight() {
        return weight;
    }

    public String getPopfile() {
        return popfile;
    }

    public String getFilename() {
        return filename;
    }

    public String getNoticeNo() {
        return noticeNo;
    }

    public String getNoticeSdt() {
        return noticeSdt;
    }

    public String getNoticeEdt() {
        return noticeEdt;
    }

    public String getSexCd() {
        return sexCd;
    }

    public String getNeuterYn() {
        return neuterYn;
    }

    public String getSpecialMark() {
        return specialMark;
    }

    public String getCareNm() {
        return careNm;
    }

    public String getCareAddr() {
        return careAddr;
    }

    public String getCareTel() {
        return careTel;
    }

    public String getCareRegNo() {
        return careRegNo;
    }

    public String getOrgNm() {
        return orgNm;
    }

    public String getSido() {
        return sido;
    }

    public LocalDateTime getCollectedAt() {
        return collectedAt;
    }

    private String text(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value).trim();
        return "null".equalsIgnoreCase(text) ? "" : text;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String normalizeSido(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String value = raw.trim();
        if (value.startsWith("서울")) return "서울특별시";
        if (value.startsWith("부산")) return "부산광역시";
        if (value.startsWith("대구")) return "대구광역시";
        if (value.startsWith("인천")) return "인천광역시";
        if (value.startsWith("광주")) return "광주광역시";
        if (value.startsWith("대전")) return "대전광역시";
        if (value.startsWith("울산")) return "울산광역시";
        if (value.startsWith("세종")) return "세종특별자치시";
        if (value.startsWith("경기")) return "경기도";
        if (value.startsWith("강원")) return "강원특별자치도";
        if (value.startsWith("충북")) return "충청북도";
        if (value.startsWith("충남")) return "충청남도";
        if (value.startsWith("전북")) return "전북특별자치도";
        if (value.startsWith("전남")) return "전라남도";
        if (value.startsWith("경북")) return "경상북도";
        if (value.startsWith("경남")) return "경상남도";
        if (value.startsWith("제주")) return "제주특별자치도";
        return value.split("\\s+")[0];
    }
}
