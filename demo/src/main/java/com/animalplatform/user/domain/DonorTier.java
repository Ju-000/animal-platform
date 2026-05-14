package com.animalplatform.user.domain;

public enum DonorTier {
    SPROUT(0L, 50_000L, "새싹 후원자", "🌱"),
    SUPPORTER(50_000L, 200_000L, "든든한 후원자", "💛"),
    CHAMPION(200_000L, 500_000L, "챔피언 후원자", "🧡"),
    ANGEL(500_000L, null, "천사 후원자", "👼");

    private final long minimumAmount;
    private final Long nextMinimumAmount;
    private final String label;
    private final String emoji;

    DonorTier(long minimumAmount, Long nextMinimumAmount, String label, String emoji) {
        this.minimumAmount = minimumAmount;
        this.nextMinimumAmount = nextMinimumAmount;
        this.label = label;
        this.emoji = emoji;
    }

    public static DonorTier fromTotalAmount(long totalAmount) {
        if (totalAmount >= ANGEL.minimumAmount) {
            return ANGEL;
        }
        if (totalAmount >= CHAMPION.minimumAmount) {
            return CHAMPION;
        }
        if (totalAmount >= SUPPORTER.minimumAmount) {
            return SUPPORTER;
        }
        return SPROUT;
    }

    public long nextTierAmount(long totalAmount) {
        if (nextMinimumAmount == null) {
            return 0L;
        }
        return Math.max(0L, nextMinimumAmount - totalAmount);
    }

    public long getMinimumAmount() {
        return minimumAmount;
    }

    public Long getNextMinimumAmount() {
        return nextMinimumAmount;
    }

    public String getLabel() {
        return label;
    }

    public String getEmoji() {
        return emoji;
    }
}
