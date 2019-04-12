var Emails = {
    key: {
        user: "%USER%",
        tenant: "%TENANT%",
        amount: "%AMOUNT%",
        currencyType: "%CURRENCYTYPE%",
        transactionId: "%TRANSACTIONID%",
        error: "%ERROR%",
    },
    get moneyPaymentFailed() {
        return {
            subject: "Shop payment failed",
            message: "Payment information:" +
                "\n\t- from user: " + this.key.user +
                "\n\t- to tenant: " + this.key.tenant +
                "\n\t- amount: " + this.key.amount +
                "\n\t- type: currency" +
                "\n\nNo previous payments to rollback." +
                "\n\nError information:\n" + this.key.error +
                "\n\n\nF4M shop service"
        }
    },
    get bonusPaymentFailed() {
        return {
            subject: "Shop payment failed",
            message: "Payment information:" +
                "\n\t- from user: " + this.key.user +
                "\n\t- to tenant: " + this.key.tenant +
                "\n\t- amount: " + this.key.amount +
                "\n\t- type: bonus points" +
                "\n\nWill atempt to rollback previous transaction " + this.key.transactionId + "." +
                "\n\nError information:\n" + this.key.error +
                "\n\n\nF4M shop service"
        }
    },
    get creditPaymentFailed() {
        return {
            subject: "Shop payment failed",
            message: "Payment information:" +
                "\n\t- from tenant: " + this.key.tenant +
                "\n\t- to user: " + this.key.user +
                "\n\t- amount: " + this.key.amount +
                "\n\t- type: credits" +
                "\n\nRewarding user with credits for purchasing an article has failed." +
                " Purchase was not canceled, so no previous payments to rollback." +
                "\n\nError information:\n" + this.key.error +
                "\n\n\nF4M shop service"
        }
    },
    get moneyRollbackSucceeded() {
        return {
            subject: "Shop payment rollback succeeded",
            message: "Payment information:" +
                "\n\t- from tenant: " + this.key.tenant +
                "\n\t- to user: " + this.key.user +
                "\n\t- amount: " + this.key.amount +
                "\n\t- type: currency" +
                "\n\nWith this payment, transaction " + this.key.transactionId + " was successfuly rolled back." +
                "\n\nError information:\n" + this.key.error +
                "\n\n\nF4M shop service"
        }
    },
    get moneyRollbackFailed() {
        return {
            subject: "Shop payment rollback failed",
            message: "Payment information:" +
                "\n\t- from tenant: " + this.key.tenant +
                "\n\t- to user: " + this.key.user +
                "\n\t- amount: " + this.key.amount +
                "\n\t- type: currency" +
                "\n\nThis was an attempt to rollback transaction " + this.key.transactionId + "." +
                "Because the rollback failed, user money account is currently unbalanced." +
                "\n\nError information:\n" + this.key.error +
                "\n\n\nF4M shop service"
        }
    },
    get bonusRollbackSucceeded() {
        return {
            subject: "Shop payment rollback succeeded",
            message: "Payment information:" +
                "\n\t- from tenant: " + this.key.tenant +
                "\n\t- to user: " + this.key.user +
                "\n\t- amount: " + this.key.amount +
                "\n\t- type: bonus points" +
                "\n\nWith this payment, transaction " + this.key.transactionId + " was successfuly rolled back." +
                "\n\nError information:\n" + this.key.error +
                "\n\n\nF4M shop service"
        }
    },
    get bonusRollbackFailed() {
        return {
            subject: "Shop payment rollback failed",
            message: "Payment information:" +
                "\n\t- from tenant: " + this.key.tenant +
                "\n\t- to user: " + this.key.user +
                "\n\t- amount: " + this.key.amount +
                "\n\t- type: bonus points" +
                "\n\nThis was an attempt to rollback transaction " + this.key.transactionId + "." +
                "Because the rollback failed, user bonus account is currently unbalanced." +
                "\n\nError information:\n" + this.key.error +
                "\n\n\nF4M shop service"
        }
    },
    get creditRollbackSucceeded() {
        return {
            subject: "Shop payment rollback succeeded",
            message: "Payment information:" +
                "\n\t- from user: " + this.key.user +
                "\n\t- to tenant: " + this.key.tenant +
                "\n\t- amount: " + this.key.amount +
                "\n\t- type: credits" +
                "\n\nWith this payment, transaction " + this.key.transactionId + " was successfuly rolled back." +
                "\n\nError information:\n" + this.key.error +
                "\n\n\nF4M shop service"
        }
    },
    get creditRollbackFailed() {
        return {
            subject: "Shop payment rollback failed",
            message: "Payment information:" +
                "\n\t- from user: " + this.key.user +
                "\n\t- to tenant: " + this.key.tenant +
                "\n\t- amount: " + this.key.amount +
                "\n\t- type: credits" +
                "\n\nThis was an attempt to rollback transaction " + this.key.transactionId + "." +
                "Because the rollback failed, tenant credit account is currently unbalanced." +
                "\n\nError information:\n" + this.key.error +
                "\n\n\nF4M shop service"
        }
    },
    get addingAnalyticEventFailed() {
        return {
            subject: "Creating analytic event for new shop order failed",
            message: "A new shop order has been completed, but adding the analytic event in Aerospike has failed." +
                "\nBecause of this, no invoice will be created in the tenant management tool. Please handle this manually." +
                "\n\nOrder information can be retrieved from OXID shop by looking for order id '" + this.key.transactionId + "'." +
                "\n\nError information:\n" + this.key.error +
                "\n\n\nF4M shop service"
        }
    }
}

module.exports = Emails;