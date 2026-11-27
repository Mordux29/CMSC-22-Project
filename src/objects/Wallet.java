package objects;

import exception.ValidationException;

// Wallet = the shop's cash on hand. The money of every sale goes in,
// and a voided transaction takes its money back out.
public class Wallet {

    // ENCAPSULATION: the balance is private, so money can only go in or out through deposit() and withdraw(),
    // which check the amount first. Nobody can set the balance to a wrong value directly.
    private double balance;

    public Wallet() {
        balance = 0;
    }

    public double getBalance() { return balance; }

    // Adds money to the wallet. The amount must be greater than 0.
    public void deposit(double amount) throws ValidationException {
        if (amount <= 0) {
            throw new ValidationException("Deposit amount must be greater than 0.");
        }
        balance = balance + amount;
    }

    // Takes money out of the wallet. Cannot take more than the balance.
    public void withdraw(double amount) throws ValidationException {
        if (amount <= 0) {
            throw new ValidationException("Withdraw amount must be greater than 0.");
        }
        if (amount > balance) {
            throw new ValidationException("Not enough cash in the wallet. Balance is only " + balance + ".");
        }
        balance = balance - amount;
    }
}
