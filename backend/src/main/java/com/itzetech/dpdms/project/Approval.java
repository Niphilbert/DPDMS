package com.itzetech.dpdms.project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "approval")
public class Approval {

    public enum Status { PENDING, APPROVED, CHANGES_REQUESTED }

    public enum Type { DESIGN, PAGE_CONTENT, DOCUMENT }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "task_id")
    private UUID taskId;

    @Column(nullable = false, length = 200)
    private String item;

    /** Lower-cased item name; versions count up per project and key. */
    @Column(name = "item_key", nullable = false, length = 200)
    private String itemKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 20)
    private Type itemType;

    @Column(name = "file_ref", length = 300)
    private String fileRef;

    @Column(nullable = false)
    private int version;

    @Column(length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(name = "answer_note", length = 1000)
    private String answerNote;

    @Column(name = "sent_by", nullable = false)
    private UUID sentBy;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt = Instant.now();

    @Column(name = "answered_by")
    private UUID answeredBy;

    @Column(name = "answered_at")
    private Instant answeredAt;

    protected Approval() {
    }

    public Approval(UUID projectId, UUID taskId, String item, Type itemType, String fileRef, int version, String message, UUID sentBy) {
        this.projectId = projectId;
        this.taskId = taskId;
        this.item = item;
        this.itemKey = item.toLowerCase();
        this.itemType = itemType;
        this.fileRef = fileRef;
        this.version = version;
        this.message = message;
        this.sentBy = sentBy;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public String getItem() {
        return item;
    }

    public String getItemKey() {
        return itemKey;
    }

    public Type getItemType() {
        return itemType;
    }

    public String getFileRef() {
        return fileRef;
    }

    public int getVersion() {
        return version;
    }

    public String getMessage() {
        return message;
    }

    public Status getStatus() {
        return status;
    }

    public String getAnswerNote() {
        return answerNote;
    }

    public UUID getSentBy() {
        return sentBy;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public UUID getAnsweredBy() {
        return answeredBy;
    }

    public Instant getAnsweredAt() {
        return answeredAt;
    }

    public void answer(Status status, UUID by, String note) {
        this.status = status;
        this.answeredBy = by;
        this.answerNote = note;
        this.answeredAt = Instant.now();
    }
}
