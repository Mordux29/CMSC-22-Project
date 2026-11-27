package objects;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Transaction is the ABSTRACT parent of everything that is recorded in the
// system (right now only Sale, but a new kind of transaction can extend it
// later). It keeps what they have in common: an ID, the
// date and time, the clerk who recorded it, and whether it was voided.
// A voided transaction is never deleted, it is only marked as voided.
// INHERITANCE: Sale extends this class. If a new kind of transaction is needed later, it can extend it too.
public abstract class Transaction {

    private String id;
    private LocalDateTime dateTime;
    private String clerkUsername;
    private boolean voided;
    private String customerName;

    public Transaction(String id, LocalDateTime dateTime, String clerkUsername, boolean voided,
                       String customerName) {
        this.id = id;
        this.dateTime = dateTime;
        this.clerkUsername = clerkUsername;
        this.voided = voided;
        this.customerName = customerName;
    }

    public String getId() { return id; }
    public LocalDateTime getDateTime() { return dateTime; }
    public String getClerkUsername() { return clerkUsername; }
    public boolean isVoided() { return voided; }
    public String getCustomerName() { return customerName; }

    // "VOIDED" or "OK", the status text shown in tables.
    // Not abstract: every kind of transaction shares this code, so it is written once in the parent.
    public String getStatus() {
        if (voided) {
            return "VOIDED";
        }
        return "OK";
    }

    // The date and time as text, always in the same format (2026-11-12 09:40:26).
    public String getDateTimeText() {
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    // Marks this transaction as voided. The record stays in the history.
    public void markVoided() {
        voided = true;
    }

    // The kind of transaction, for example "Sale".
    // ABSTRACTION: these abstract methods are only declared here. Each child (like Sale) writes the real code.
    public abstract String getType();

    // The cash value that goes into the wallet.
    public abstract double getAmount();

    // A short text describing what was sold.
    public abstract String getDetails();

    // One or more lines used to save this transaction in the file.
    public abstract String toFileString();

    // The first fields every saved line shares: id|dateTime|clerk|voided|customer
    protected String baseFileString() {
        return id + "|" + dateTime + "|" + clerkUsername + "|" + voided + "|" + customerName;
    }
}
