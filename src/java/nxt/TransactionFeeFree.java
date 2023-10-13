package nxt;

class TransactionFeeFree extends TransactionImpl {

    public TransactionFeeFree(BuilderImpl builder, String secretPhrase) throws NxtException.NotValidException {
        super(builder, secretPhrase);
    }

    @Override
    public void validate() throws NxtException.ValidationException {
        if (timestamp == 0 ? (deadline != 0 || feeNQT != 0) : (deadline < 1 || feeNQT < 0)
                || feeNQT > Constants.MAX_BALANCE_NQT
                || amountNQT < 0
                || amountNQT > Constants.MAX_BALANCE_NQT
                || type == null) {
            throw new NxtException.NotValidException(
                    "Invalid transaction parameters:\n type: " + type + ", timestamp: " + timestamp
                            + ", deadline: " + deadline + ", fee: " + feeNQT + ", amount: " + amountNQT);
        }

        validateInternal();
    }
}
