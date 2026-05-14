package com.animalplatform.chat.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "chat_quick_answers")
public class ChatQuickAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String buttonLabel;

    @Column(nullable = false, unique = true, length = 120)
    private String triggerKeyword;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answerText;

    @Column(nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean active = true;

    protected ChatQuickAnswer() {
    }

    public ChatQuickAnswer(String buttonLabel, String triggerKeyword, String answerText, int displayOrder, boolean active) {
        this.buttonLabel = buttonLabel;
        this.triggerKeyword = triggerKeyword;
        this.answerText = answerText;
        this.displayOrder = displayOrder;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getButtonLabel() {
        return buttonLabel;
    }

    public String getTriggerKeyword() {
        return triggerKeyword;
    }

    public String getAnswerText() {
        return answerText;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public boolean isActive() {
        return active;
    }
}
