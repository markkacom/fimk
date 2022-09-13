package nxt.txn.extension;

import nxt.Attachment;
import nxt.MofoAttachment;
import nxt.Transaction;

abstract class NamespacedAliasBasedExtension extends TransactionTypeExtension {

    public String validate(Transaction transaction) {
        MofoAttachment.NamespacedAliasAssignmentAttachment a;
        Attachment att = transaction.getAttachment();
        if (att instanceof MofoAttachment.NamespacedAliasAssignmentAttachment) {
            a = (MofoAttachment.NamespacedAliasAssignmentAttachment) att;
        } else {
            return "Attachment type is not suitable";
        }
        if (!getMark().equals(a.getAliasName())) return "Wrong mark";
        return null;
    }

}
