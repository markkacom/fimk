/******************************************************************************
 * Copyright © 2013-2015 The Nxt Core Developers.                             *
 *                                                                            *
 * See the AUTHORS.txt, DEVELOPER-AGREEMENT.txt and LICENSE.txt files at      *
 * the top-level directory of this distribution for the individual copyright  *
 * holder information and the developer policies on copyright and licensing.  *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * Nxt software, including this file, may be copied, modified, propagated,    *
 * or distributed except according to the terms contained in the LICENSE.txt  *
 * file.                                                                      *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

package nxt;

import nxt.crypto.Crypto;
import nxt.crypto.EncryptedData;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public interface Attachment extends Appendix {

    TransactionType getTransactionType();

    public abstract static class AbstractAttachment extends Appendix.AbstractAppendix implements Attachment {

        protected AbstractAttachment(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
        }

        protected AbstractAttachment(JSONObject attachmentData) {
            super(attachmentData);
        }

        private AbstractAttachment(int version) {
            super(version);
        }

        protected AbstractAttachment() {}

        @Override
        final String getAppendixName() {
            return getTransactionType().getName();
        }

        @Override
        final void validate(Transaction transaction) throws NxtException.ValidationException {
            getTransactionType().validateAttachment(transaction);
        }

        @Override
        final void validateAtFinish(Transaction transaction) throws NxtException.ValidationException {
            getTransactionType().validateAttachmentAtFinish(transaction);
        }

        final void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
            getTransactionType().apply((TransactionImpl)transaction, senderAccount, recipientAccount);
        }

        @Override
        public Fee getBaselineFee(Transaction transaction) {
            return getTransactionType().getBaselineFee(transaction);
        }

        @Override
        final boolean isPhasable() {
            return getTransactionType().isPhasable();
        }

    }

    abstract class EmptyAttachment extends AbstractAttachment {

        protected EmptyAttachment() {
            super(0);
        }

        @Override
        protected final int getMySize() {
            return 0;
        }

        @Override
        protected final void putMyBytes(ByteBuffer buffer) {
        }

        @Override
        protected final void putMyJSON(JSONObject json) {
        }

        @Override
        final boolean verifyVersion(byte transactionVersion) {
            return true;
        }

    }

    EmptyAttachment ORDINARY_PAYMENT = new EmptyAttachment() {

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Payment.ORDINARY;
        }

    };

    // the message payload is in the Appendix
    EmptyAttachment ARBITRARY_MESSAGE = new EmptyAttachment() {

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.ARBITRARY_MESSAGE;
        }

    };

    final class MessagingAliasAssignment extends AbstractAttachment {

        private final String aliasName;
        private final String aliasURI;

        MessagingAliasAssignment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            super(buffer, transactionVersion);
            aliasName = Convert.readString(buffer, buffer.get(), Constants.MAX_ALIAS_LENGTH).trim();
            aliasURI = Convert.readString(buffer, buffer.getShort(), Constants.MAX_ALIAS_URI_LENGTH).trim();
        }

        MessagingAliasAssignment(JSONObject attachmentData) {
            super(attachmentData);
            aliasName = (Convert.nullToEmpty((String) attachmentData.get("alias"))).trim();
            aliasURI = (Convert.nullToEmpty((String) attachmentData.get("uri"))).trim();
        }

        public MessagingAliasAssignment(String aliasName, String aliasURI) {
            this.aliasName = aliasName.trim();
            this.aliasURI = aliasURI.trim();
        }

        @Override
        protected int getMySize() {
            return 1 + Convert.toBytes(aliasName).length + 2 + Convert.toBytes(aliasURI).length;
        }

        @Override
        protected void putMyBytes(ByteBuffer buffer) {
            byte[] alias = Convert.toBytes(this.aliasName);
            byte[] uri = Convert.toBytes(this.aliasURI);
            buffer.put((byte)alias.length);
            buffer.put(alias);
            buffer.putShort((short) uri.length);
            buffer.put(uri);
        }

        @Override
        protected void putMyJSON(JSONObject attachment) {
            attachment.put("alias", aliasName);
            attachment.put("uri", aliasURI);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.ALIAS_ASSIGNMENT;
        }

        public String getAliasName() {
            return aliasName;
        }

        public String getAliasURI() {
            return aliasURI;
        }
    }

    final class MessagingAliasSell extends AbstractAttachment {

        private final String aliasName;
        private final long priceNQT;

        MessagingAliasSell(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            super(buffer, transactionVersion);
            this.aliasName = Convert.readString(buffer, buffer.get(), Constants.MAX_ALIAS_LENGTH);
            this.priceNQT = buffer.getLong();
        }

        MessagingAliasSell(JSONObject attachmentData) {
            super(attachmentData);
            this.aliasName = Convert.nullToEmpty((String) attachmentData.get("alias"));
            this.priceNQT = Convert.parseLong(attachmentData.get("priceNQT"));
        }

        public MessagingAliasSell(String aliasName, long priceNQT) {
            this.aliasName = aliasName;
            this.priceNQT = priceNQT;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.ALIAS_SELL;
        }

        @Override
        protected int getMySize() {
            return 1 + Convert.toBytes(aliasName).length + 8;
        }

        @Override
        protected void putMyBytes(ByteBuffer buffer) {
            byte[] aliasBytes = Convert.toBytes(aliasName);
            buffer.put((byte)aliasBytes.length);
            buffer.put(aliasBytes);
            buffer.putLong(priceNQT);
        }

        @Override
        protected void putMyJSON(JSONObject attachment) {
            attachment.put("alias", aliasName);
            attachment.put("priceNQT", priceNQT);
        }

        public String getAliasName(){
            return aliasName;
        }

        public long getPriceNQT(){
            return priceNQT;
        }
    }

    final class MessagingAliasBuy extends AbstractAttachment {

        private final String aliasName;

        MessagingAliasBuy(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            super(buffer, transactionVersion);
            this.aliasName = Convert.readString(buffer, buffer.get(), Constants.MAX_ALIAS_LENGTH);
        }

        MessagingAliasBuy(JSONObject attachmentData) {
            super(attachmentData);
            this.aliasName = Convert.nullToEmpty((String) attachmentData.get("alias"));
        }

        public MessagingAliasBuy(String aliasName) {
            this.aliasName = aliasName;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.ALIAS_BUY;
        }

        @Override
        protected int getMySize() {
            return 1 + Convert.toBytes(aliasName).length;
        }

        @Override
        protected void putMyBytes(ByteBuffer buffer) {
            byte[] aliasBytes = Convert.toBytes(aliasName);
            buffer.put((byte) aliasBytes.length);
            buffer.put(aliasBytes);
        }

        @Override
        protected void putMyJSON(JSONObject attachment) {
            attachment.put("alias", aliasName);
        }

        public String getAliasName(){
            return aliasName;
        }
    }

    final class MessagingAliasDelete extends AbstractAttachment {

        private final String aliasName;

        MessagingAliasDelete(final ByteBuffer buffer, final byte transactionVersion) throws NxtException.NotValidException {
            super(buffer, transactionVersion);
            this.aliasName = Convert.readString(buffer, buffer.get(), Constants.MAX_ALIAS_LENGTH);
        }

        MessagingAliasDelete(final JSONObject attachmentData) {
            super(attachmentData);
            this.aliasName = Convert.nullToEmpty((String) attachmentData.get("alias"));
        }

        public MessagingAliasDelete(final String aliasName) {
            this.aliasName = aliasName;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.ALIAS_DELETE;
        }

        @Override
        protected int getMySize() {
            return 1 + Convert.toBytes(aliasName).length;
        }

        @Override
        protected void putMyBytes(final ByteBuffer buffer) {
            byte[] aliasBytes = Convert.toBytes(aliasName);
            buffer.put((byte)aliasBytes.length);
            buffer.put(aliasBytes);
        }

        @Override
        protected void putMyJSON(final JSONObject attachment) {
            attachment.put("alias", aliasName);
        }

        public String getAliasName(){
            return aliasName;
        }
    }

    final class MessagingPollCreation extends AbstractAttachment {

        public final static class PollBuilder {
            private final String pollName;
            private final String pollDescription;
            private final String[] pollOptions;

            private final int finishHeight;
            private final byte votingModel;

            private long minBalance = 0;
            private byte minBalanceModel;

            private final byte minNumberOfOptions;
            private final byte maxNumberOfOptions;

            private final byte minRangeValue;
            private final byte maxRangeValue;

            private long holdingId;

            public PollBuilder(final String pollName, final String pollDescription, final String[] pollOptions,
                               final int finishHeight, final byte votingModel,
                               byte minNumberOfOptions, byte maxNumberOfOptions,
                               byte minRangeValue, byte maxRangeValue) {
                this.pollName = pollName;
                this.pollDescription = pollDescription;
                this.pollOptions = pollOptions;

                this.finishHeight = finishHeight;
                this.votingModel = votingModel;
                this.minNumberOfOptions = minNumberOfOptions;
                this.maxNumberOfOptions = maxNumberOfOptions;
                this.minRangeValue = minRangeValue;
                this.maxRangeValue = maxRangeValue;

                this.minBalanceModel = VoteWeighting.VotingModel.get(votingModel).getMinBalanceModel().getCode();
            }

            public PollBuilder minBalance(byte minBalanceModel, long minBalance) {
                this.minBalanceModel = minBalanceModel;
                this.minBalance = minBalance;
                return this;
            }

            public PollBuilder holdingId(long holdingId) {
                this.holdingId = holdingId;
                return this;
            }

            public MessagingPollCreation build() {
                return new MessagingPollCreation(this);
            }
        }

        private final String pollName;
        private final String pollDescription;
        private final String[] pollOptions;

        private final int finishHeight;

        private final byte minNumberOfOptions;
        private final byte maxNumberOfOptions;
        private final byte minRangeValue;
        private final byte maxRangeValue;
        private final VoteWeighting voteWeighting;

        MessagingPollCreation(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            super(buffer, transactionVersion);
            this.pollName = Convert.readString(buffer, buffer.getShort(), Constants.MAX_POLL_NAME_LENGTH);
            this.pollDescription = Convert.readString(buffer, buffer.getShort(), Constants.MAX_POLL_DESCRIPTION_LENGTH);

            this.finishHeight = buffer.getInt();

            int numberOfOptions = buffer.get();
            if (numberOfOptions > Constants.MAX_POLL_OPTION_COUNT) {
                throw new NxtException.NotValidException("Invalid number of poll options: " + numberOfOptions);
            }

            this.pollOptions = new String[numberOfOptions];
            for (int i = 0; i < numberOfOptions; i++) {
                this.pollOptions[i] = Convert.readString(buffer, buffer.getShort(), Constants.MAX_POLL_OPTION_LENGTH);
            }

            byte votingModel = buffer.get();

            this.minNumberOfOptions = buffer.get();
            this.maxNumberOfOptions = buffer.get();

            this.minRangeValue = buffer.get();
            this.maxRangeValue = buffer.get();

            long minBalance = buffer.getLong();
            byte minBalanceModel = buffer.get();
            long holdingId = buffer.getLong();
            this.voteWeighting = new VoteWeighting(votingModel, holdingId, minBalance, minBalanceModel);
        }

        MessagingPollCreation(JSONObject attachmentData) {
            super(attachmentData);

            this.pollName = ((String) attachmentData.get("name")).trim();
            this.pollDescription = ((String) attachmentData.get("description")).trim();
            this.finishHeight = ((Long) attachmentData.get("finishHeight")).intValue();

            JSONArray options = (JSONArray) attachmentData.get("options");
            this.pollOptions = new String[options.size()];
            for (int i = 0; i < pollOptions.length; i++) {
                this.pollOptions[i] = ((String) options.get(i)).trim();
            }
            byte votingModel = ((Long) attachmentData.get("votingModel")).byteValue();

            this.minNumberOfOptions = ((Long) attachmentData.get("minNumberOfOptions")).byteValue();
            this.maxNumberOfOptions = ((Long) attachmentData.get("maxNumberOfOptions")).byteValue();
            this.minRangeValue = ((Long) attachmentData.get("minRangeValue")).byteValue();
            this.maxRangeValue = ((Long) attachmentData.get("maxRangeValue")).byteValue();

            long minBalance = Convert.parseLong(attachmentData.get("minBalance"));
            byte minBalanceModel = ((Long) attachmentData.get("minBalanceModel")).byteValue();
            long holdingId = Convert.parseUnsignedLong((String) attachmentData.get("holding"));
            this.voteWeighting = new VoteWeighting(votingModel, holdingId, minBalance, minBalanceModel);
        }

        private MessagingPollCreation(PollBuilder builder) {
            this.pollName = builder.pollName;
            this.pollDescription = builder.pollDescription;
            this.pollOptions = builder.pollOptions;
            this.finishHeight = builder.finishHeight;
            this.minNumberOfOptions = builder.minNumberOfOptions;
            this.maxNumberOfOptions = builder.maxNumberOfOptions;
            this.minRangeValue = builder.minRangeValue;
            this.maxRangeValue = builder.maxRangeValue;
            this.voteWeighting = new VoteWeighting(builder.votingModel, builder.holdingId, builder.minBalance, builder.minBalanceModel);
        }

        @Override
        protected int getMySize() {
            int size = 2 + Convert.toBytes(pollName).length + 2 + Convert.toBytes(pollDescription).length + 1;
            for (String pollOption : pollOptions) {
                size += 2 + Convert.toBytes(pollOption).length;
            }

            size += 4 + 1 + 1 + 1 + 1 + 1 + 8 + 1 + 8;

            return size;
        }

        @Override
        protected void putMyBytes(ByteBuffer buffer) {
            byte[] name = Convert.toBytes(this.pollName);
            byte[] description = Convert.toBytes(this.pollDescription);
            byte[][] options = new byte[this.pollOptions.length][];
            for (int i = 0; i < this.pollOptions.length; i++) {
                options[i] = Convert.toBytes(this.pollOptions[i]);
            }

            buffer.putShort((short) name.length);
            buffer.put(name);
            buffer.putShort((short) description.length);
            buffer.put(description);
            buffer.putInt(finishHeight);
            buffer.put((byte) options.length);
            for (byte[] option : options) {
                buffer.putShort((short) option.length);
                buffer.put(option);
            }
            buffer.put(this.voteWeighting.getVotingModel().getCode());

            buffer.put(this.minNumberOfOptions);
            buffer.put(this.maxNumberOfOptions);
            buffer.put(this.minRangeValue);
            buffer.put(this.maxRangeValue);

            buffer.putLong(this.voteWeighting.getMinBalance());
            buffer.put(this.voteWeighting.getMinBalanceModel().getCode());
            buffer.putLong(this.voteWeighting.getHoldingId());
        }

        @Override
        protected void putMyJSON(JSONObject attachment) {
            attachment.put("name", this.pollName);
            attachment.put("description", this.pollDescription);
            attachment.put("finishHeight", this.finishHeight);
            JSONArray options = new JSONArray();
            if (this.pollOptions != null) {
                Collections.addAll(options, this.pollOptions);
            }
            attachment.put("options", options);


            attachment.put("minNumberOfOptions", this.minNumberOfOptions);
            attachment.put("maxNumberOfOptions", this.maxNumberOfOptions);

            attachment.put("minRangeValue", this.minRangeValue);
            attachment.put("maxRangeValue", this.maxRangeValue);

            attachment.put("votingModel", this.voteWeighting.getVotingModel().getCode());

            attachment.put("minBalance", this.voteWeighting.getMinBalance());
            attachment.put("minBalanceModel", this.voteWeighting.getMinBalanceModel().getCode());
            attachment.put("holding", Long.toUnsignedString(this.voteWeighting.getHoldingId()));
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.POLL_CREATION;
        }

        public String getPollName() {
            return pollName;
        }

        public String getPollDescription() {
            return pollDescription;
        }

        public int getFinishHeight() {
            return finishHeight;
        }

        public String[] getPollOptions() {
            return pollOptions;
        }

        public byte getMinNumberOfOptions() {
            return minNumberOfOptions;
        }

        public byte getMaxNumberOfOptions() {
            return maxNumberOfOptions;
        }

        public byte getMinRangeValue() {
            return minRangeValue;
        }

        public byte getMaxRangeValue() {
            return maxRangeValue;
        }

        public VoteWeighting getVoteWeighting() {
            return voteWeighting;
        }

    }

    final class MessagingVoteCasting extends AbstractAttachment {

        private final long pollId;
        private final byte[] pollVote;

        public MessagingVoteCasting(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            super(buffer, transactionVersion);
            pollId = buffer.getLong();
            int numberOfOptions = buffer.get();
            if (numberOfOptions > Constants.MAX_POLL_OPTION_COUNT) {
                throw new NxtException.NotValidException("More than " + Constants.MAX_POLL_OPTION_COUNT + " options in a vote");
            }
            pollVote = new byte[numberOfOptions];
            buffer.get(pollVote);
        }

        public MessagingVoteCasting(JSONObject attachmentData) {
            super(attachmentData);
            pollId = Convert.parseUnsignedLong((String) attachmentData.get("poll"));
            JSONArray vote = (JSONArray) attachmentData.get("vote");
            pollVote = new byte[vote.size()];
            for (int i = 0; i < pollVote.length; i++) {
                pollVote[i] = ((Long) vote.get(i)).byteValue();
            }
        }

        public MessagingVoteCasting(long pollId, byte[] pollVote) {
            this.pollId = pollId;
            this.pollVote = pollVote;
        }

        @Override
        protected int getMySize() {
            return 8 + 1 + this.pollVote.length;
        }

        @Override
        protected void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(this.pollId);
            buffer.put((byte) this.pollVote.length);
            buffer.put(this.pollVote);
        }

        @Override
        protected void putMyJSON(JSONObject attachment) {
            attachment.put("poll", Long.toUnsignedString(this.pollId));
            JSONArray vote = new JSONArray();
            if (this.pollVote != null) {
                for (byte aPollVote : this.pollVote) {
                    vote.add(aPollVote);
                }
            }
            attachment.put("vote", vote);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.VOTE_CASTING;
        }

        public long getPollId() {
            return pollId;
        }

        public byte[] getPollVote() {
            return pollVote;
        }
    }

    final class MessagingPhasingVoteCasting extends AbstractAttachment {

        private final List<byte[]> transactionFullHashes;
        private final byte[] revealedSecret;

        MessagingPhasingVoteCasting(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            super(buffer, transactionVersion);
            byte length = buffer.get();
            transactionFullHashes = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                byte[] hash = new byte[32];
                buffer.get(hash);
                transactionFullHashes.add(hash);
            }
            int secretLength = buffer.getInt();
            if (secretLength > Constants.MAX_PHASING_REVEALED_SECRET_LENGTH) {
                throw new NxtException.NotValidException("Invalid revealed secret length " + secretLength);
            }
            if (secretLength > 0) {
                revealedSecret = new byte[secretLength];
                buffer.get(revealedSecret);
            } else {
                revealedSecret = Convert.EMPTY_BYTE;
            }
        }

        MessagingPhasingVoteCasting(JSONObject attachmentData) {
            super(attachmentData);
            JSONArray hashes = (JSONArray) attachmentData.get("transactionFullHashes");
            transactionFullHashes = new ArrayList<>(hashes.size());
            hashes.forEach(hash -> transactionFullHashes.add(Convert.parseHexString((String) hash)));
            String revealedSecret = Convert.emptyToNull((String) attachmentData.get("revealedSecret"));
            this.revealedSecret = revealedSecret != null ? Convert.parseHexString(revealedSecret) : Convert.EMPTY_BYTE;
        }

        public MessagingPhasingVoteCasting(List<byte[]> transactionFullHashes, byte[] revealedSecret) {
            this.transactionFullHashes = transactionFullHashes;
            this.revealedSecret = revealedSecret;
        }

        @Override
        protected int getMySize() {
            return 1 + 32 * transactionFullHashes.size() + 4 + revealedSecret.length;
        }

        @Override
        protected void putMyBytes(ByteBuffer buffer) {
            buffer.put((byte) transactionFullHashes.size());
            transactionFullHashes.forEach(buffer::put);
            buffer.putInt(revealedSecret.length);
            buffer.put(revealedSecret);
        }

        @Override
        protected void putMyJSON(JSONObject attachment) {
            JSONArray jsonArray = new JSONArray();
            transactionFullHashes.forEach(hash -> jsonArray.add(Convert.toHexString(hash)));
            attachment.put("transactionFullHashes", jsonArray);
            if (revealedSecret.length > 0) {
                attachment.put("revealedSecret", Convert.toHexString(revealedSecret));
            }
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.PHASING_VOTE_CASTING;
        }

        public List<byte[]> getTransactionFullHashes() {
            return transactionFullHashes;
        }

        public byte[] getRevealedSecret() {
            return revealedSecret;
        }
    }

    final class MessagingHubAnnouncement extends AbstractAttachment {

        private final long minFeePerByteNQT;
        private final String[] uris;

        MessagingHubAnnouncement(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            super(buffer, transactionVersion);
            this.minFeePerByteNQT = buffer.getLong();
            int numberOfUris = buffer.get();
            if (numberOfUris > Constants.MAX_HUB_ANNOUNCEMENT_URIS) {
                throw new NxtException.NotValidException("Invalid number of URIs: " + numberOfUris);
            }
            this.uris = new String[numberOfUris];
            for (int i = 0; i < uris.length; i++) {
                uris[i] = Convert.readString(buffer, buffer.getShort(), Constants.MAX_HUB_ANNOUNCEMENT_URI_LENGTH);
            }
        }

        MessagingHubAnnouncement(JSONObject attachmentData) throws NxtException.NotValidException {
            super(attachmentData);
            this.minFeePerByteNQT = (Long) attachmentData.get("minFeePerByte");
            try {
                JSONArray urisData = (JSONArray) attachmentData.get("uris");
                this.uris = new String[urisData.size()];
                for (int i = 0; i < uris.length; i++) {
                    uris[i] = (String) urisData.get(i);
                }
            } catch (RuntimeException e) {
                throw new NxtException.NotValidException("Error parsing hub terminal announcement parameters", e);
            }
        }

        public MessagingHubAnnouncement(long minFeePerByteNQT, String[] uris) {
            this.minFeePerByteNQT = minFeePerByteNQT;
            this.uris = uris;
        }

        @Override
        protected int getMySize() {
            int size = 8 + 1;
            for (String uri : uris) {
                size += 2 + Convert.toBytes(uri).length;
            }
            return size;
        }

        @Override
        protected void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(minFeePerByteNQT);
            buffer.put((byte) uris.length);
            for (String uri : uris) {
                byte[] uriBytes = Convert.toBytes(uri);
                buffer.putShort((short)uriBytes.length);
                buffer.put(uriBytes);
            }
        }

        @Override
        protected void putMyJSON(JSONObject attachment) {
            attachment.put("minFeePerByteNQT", minFeePerByteNQT);
            JSONArray uris = new JSONArray();
            Collections.addAll(uris, this.uris);
            attachment.put("uris", uris);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.HUB_ANNOUNCEMENT;
        }

        public long getMinFeePerByteNQT() {
            return minFeePerByteNQT;
        }

        public String[] getUris() {
            return uris;
        }

    }

    final class MessagingAccountInfo extends AbstractAttachment {

        private final String name;
        private final String description;

        MessagingAccountInfo(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            super(buffer, transactionVersion);
            this.name = Convert.readString(buffer, buffer.get(), Constants.MAX_ACCOUNT_NAME_LENGTH);
            this.description = Convert.readString(buffer, buffer.getShort(), Constants.MAX_ACCOUNT_DESCRIPTION_LENGTH);
        }

        MessagingAccountInfo(JSONObject attachmentData) {
            super(attachmentData);
            this.name = Convert.nullToEmpty((String) attachmentData.get("name"));
            this.description = Convert.nullToEmpty((String) attachmentData.get("description"));
        }

        public MessagingAccountInfo(String name, String description) {
            super(1);
            this.name = name;
            this.description = description;
        }

        @Override
        protected int getMySize() {
            return 1 + Convert.toBytes(name).length + 2 + Convert.toBytes(description).length;
        }

        @Override
        protected void putMyBytes(ByteBuffer buffer) {
            byte[] name = Convert.toBytes(this.name);
            byte[] description = Convert.toBytes(this.description);
            buffer.put((byte)name.length);
            buffer.put(name);
            buffer.putShort((short) description.length);
            buffer.put(description);
        }

        @Override
        protected void putMyJSON(JSONObject attachment) {
            attachment.put("name", name);
            attachment.put("description", description);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.ACCOUNT_INFO;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

    }

    final class DigitalGoodsListing extends AbstractAttachment {

        private final String name;
        private final String description;
        private final String tags;
        private final int quantity;
        private final long priceNQT;

        DigitalGoodsListing(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            super(buffer, transactionVersion);
            this.name = Convert.readString(buffer, buffer.getShort(), Constants.MAX_DGS_LISTING_NAME_LENGTH);
            this.description = Convert.readString(buffer, buffer.getShort(), Constants.MAX_DGS_LISTING_DESCRIPTION_LENGTH);
            this.tags = Convert.readString(buffer, buffer.getShort(), Constants.MAX_DGS_LISTING_TAGS_LENGTH);
            this.quantity = buffer.getInt();
            this.priceNQT = buffer.getLong();
        }

        DigitalGoodsListing(JSONObject attachmentData) {
            super(attachmentData);
            this.name = (String) attachmentData.get("name");
            this.description = (String) attachmentData.get("description");
            this.tags = (String) attachmentData.get("tags");
            this.quantity = ((Long) attachmentData.get("quantity")).intValue();
            this.priceNQT = Convert.parseLong(attachmentData.get("priceNQT"));
        }

        public DigitalGoodsListing(String name, String description, String tags, int quantity, long priceNQT) {
            this.name = name;
            this.description = description;
            this.tags = tags;
            this.quantity = quantity;
            this.priceNQT = priceNQT;
        }

        @Override
        protected int getMySize() {
            return 2 + Convert.toBytes(name).length + 2 + Convert.toBytes(description).length + 2
                        + Convert.toBytes(tags).length + 4 + 8;
        }

        @Override
        protected void putMyBytes(ByteBuffer buffer) {
            byte[] nameBytes = Convert.toBytes(name);
            buffer.putShort((short) nameBytes.length);
            buffer.put(nameBytes);
            byte[] descriptionBytes = Convert.toBytes(description);
            buffer.putShort((short) descriptionBytes.length);
            buffer.put(descriptionBytes);
            byte[] tagsBytes = Convert.toBytes(tags);
            buffer.putShort((short) tagsBytes.length);
            buffer.put(tagsBytes);
            buffer.putInt(quantity);
            buffer.putLong(priceNQT);
        }

        @Override
        protected void putMyJSON(JSONObject attachment) {
            attachment.put("name", name);
            attachment.put("description", description);
            attachment.put("tags", tags);
            attachment.put("quantity", quantity);
            attachment.put("priceNQT", priceNQT);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.LISTING;
        }

        public String getName() { return name; }

        public String getDescription() { return description; }

        public String getTags() { return tags; }

        public int getQuantity() { return quantity; }

        public long getPriceNQT() { return priceNQT; }

    }

    final class DigitalGoodsDelisting extends AbstractAttachment {

        private final long goodsId;

        DigitalGoodsDelisting(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.goodsId = buffer.getLong();
        }

        DigitalGoodsDelisting(JSONObject attachmentData) {
            super(attachmentData);
            this.goodsId = Convert.parseUnsignedLong((String)attachmentData.get("goods"));
        }

        public DigitalGoodsDelisting(long goodsId) {
            this.goodsId = goodsId;
        }

        @Override
        protected int getMySize() {
            return 8;
        }

        @Override
        protected void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(goodsId);
        }

        @Override
        protected void putMyJSON(JSONObject attachment) {
            attachment.put("goods", Long.toUnsignedString(goodsId));
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.DELISTING;
        }

        public long getGoodsId() { return goodsId; }

    }

    final class DigitalGoodsPriceChange extends AbstractAttachment {

        private final long goodsId;
        private final long priceNQT;

        DigitalGoodsPriceChange(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.goodsId = buffer.getLong();
            this.priceNQT = buffer.getLong();
        }

        DigitalGoodsPriceChange(JSONObject attachmentData) {
            super(attachmentData);
            this.goodsId = Convert.parseUnsignedLong((String)attachmentData.get("goods"));
            this.priceNQT = Convert.parseLong(attachmentData.get("priceNQT"));
        }

        public DigitalGoodsPriceChange(long goodsId, long priceNQT) {
            this.goodsId = goodsId;
            this.priceNQT = priceNQT;
        }

        @Override
        protected int getMySize() {
            return 8 + 8;
        }

        @Override
        protected void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(goodsId);
            buffer.putLong(priceNQT);
        }

        @Override
        protected void putMyJSON(JSONObject attachment) {
            attachment.put("goods", Long.toUnsignedString(goodsId));
            attachment.put("priceNQT", priceNQT);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.PRICE_CHANGE;
        }

        public long getGoodsId() { return goodsId; }

        public long getPriceNQT() { return priceNQT; }

    }

    final class DigitalGoodsQuantityChange extends AbstractAttachment {

        private final long goodsId;
        private final int deltaQuantity;

        DigitalGoodsQuantityChange(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.goodsId = buffer.getLong();
            this.deltaQuantity = buffer.getInt();
        }

        DigitalGoodsQuantityChange(JSONObject attachmentData) {
            super(attachmentData);
            this.goodsId = Convert.parseUnsignedLong((String)attachmentData.get("goods"));
            this.deltaQuantity = ((Long)attachmentData.get("deltaQuantity")).intValue();
        }

        public DigitalGoodsQuantityChange(long goodsId, int deltaQuantity) {
            this.goodsId = goodsId;
            this.deltaQuantity = deltaQuantity;
        }

        @Override
        protected int getMySize() {
            return 8 + 4;
        }

        @Override
        protected void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(goodsId);
            buffer.putInt(deltaQuantity);
        }

        @Override
        protected void putMyJSON(JSONObject attachment) {
            attachment.put("goods", Long.toUnsignedString(goodsId));
            attachment.put("deltaQuantity", deltaQuantity);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.QUANTITY_CHANGE;
        }

        public long getGoodsId() { return goodsId; }

        public int getDeltaQuantity() { return deltaQuantity; }

    }

    final class DigitalGoodsPurchase extends AbstractAttachment {

        private final long goodsId;
        private final int quantity;
        private final long priceNQT;
        private final int deliveryDeadlineTimestamp;

        DigitalGoodsPurchase(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.goodsId = buffer.getLong();
            this.quantity = buffer.getInt();
            this.priceNQT = buffer.getLong();
            this.deliveryDeadlineTimestamp = buffer.getInt();
        }

        DigitalGoodsPurchase(JSONObject attachmentData) {
            super(attachmentData);
            this.goodsId = Convert.parseUnsignedLong((String)attachmentData.get("goods"));
            this.quantity = ((Long)attachmentData.get("quantity")).intValue();
            this.priceNQT = Convert.parseLong(attachmentData.get("priceNQT"));
            this.deliveryDeadlineTimestamp = ((Long)attachmentData.get("deliveryDeadlineTimestamp")).intValue();
        }

        public DigitalGoodsPurchase(long goodsId, int quantity, long priceNQT, int deliveryDeadlineTimestamp) {
            this.goodsId = goodsId;
            this.quantity = quantity;
            this.priceNQT = priceNQT;
            this.deliveryDeadlineTimestamp = deliveryDeadlineTimestamp;
        }

        @Override
        protected int getMySize() {
            return 8 + 4 + 8 + 4;
        }

        @Override
        protected void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(goodsId);
            buffer.putInt(quantity);
            buffer.putLong(priceNQT);
            buffer.putInt(deliveryDeadlineTimestamp);
        }

        @Override
        protected void putMyJSON(JSONObject attachment) {
            attachment.put("goods", Long.toUnsignedString(goodsId));
            attachment.put("quantity", quantity);
            attachment.put("priceNQT", priceNQT);
            attachment.put("deliveryDeadlineTimestamp", deliveryDeadlineTimestamp);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.PURCHASE;
        }

        public long getGoodsId() { return goodsId; }

        public int getQuantity() { return quantity; }

        public long getPriceNQT() { return priceNQT; }

        public int getDeliveryDeadlineTimestamp() { return deliveryDeadlineTimestamp; }

    }

    final class DigitalGoodsDelivery extends AbstractAttachment {

        private final long purchaseId;
        private final EncryptedData goods;
        private final long discountNQT;
        private final boolean goodsIsText;

        DigitalGoodsDelivery(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            super(buffer, transactionVersion);
            this.purchaseId = buffer.getLong();
            int length = buffer.getInt();
            goodsIsText = length < 0;
            if (length < 0) {
                length &= Integer.MAX_VALUE;
            }
            this.goods = EncryptedData.readEncryptedData(buffer, length, Constants.MAX_DGS_GOODS_LENGTH);
            this.discountNQT = buffer.getLong();
        }

        DigitalGoodsDelivery(JSONObject attachmentData) {
            super(attachmentData);
            this.purchaseId = Convert.parseUnsignedLong((String) attachmentData.get("purchase"));
            this.goods = new EncryptedData(Convert.parseHexString((String)attachmentData.get("goodsData")),
                    Convert.parseHexString((String)attachmentData.get("goodsNonce")));
            this.discountNQT = Convert.parseLong(attachmentData.get("discountNQT"));
            this.goodsIsText = Boolean.TRUE.equals(attachmentData.get("goodsIsText"));
        }

        public DigitalGoodsDelivery(long purchaseId, EncryptedData goods, boolean goodsIsText, long discountNQT) {
            this.purchaseId = purchaseId;
            this.goods = goods;
            this.discountNQT = discountNQT;
            this.goodsIsText = goodsIsText;
        }

        @Override
        protected int getMySize() {
            return 8 + 4 + goods.getSize() + 8;
        }

        @Override
        protected void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(purchaseId);
            buffer.putInt(goodsIsText ? goods.getData().length | Integer.MIN_VALUE : goods.getData().length);
            buffer.put(goods.getData());
            buffer.put(goods.getNonce());
            buffer.putLong(discountNQT);
        }

        @Override
        protected void putMyJSON(JSONObject attachment) {
            attachment.put("purchase", Long.toUnsignedString(purchaseId));
            attachment.put("goodsData", Convert.toHexString(goods.getData()));
            attachment.put("goodsNonce", Convert.toHexString(goods.getNonce()));
            attachment.put("discountNQT", discountNQT);
            attachment.put("goodsIsText", goodsIsText);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.DELIVERY;
        }

        public long getPurchaseId() { return purchaseId; }

        public EncryptedData getGoods() { return goods; }

        public long getDiscountNQT() { return discountNQT; }

        public boolean goodsIsText() {
            return goodsIsText;
        }

    }

    final class DigitalGoodsFeedback extends AbstractAttachment {

        private final long purchaseId;

        DigitalGoodsFeedback(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.purchaseId = buffer.getLong();
        }

        DigitalGoodsFeedback(JSONObject attachmentData) {
            super(attachmentData);
            this.purchaseId = Convert.parseUnsignedLong((String)attachmentData.get("purchase"));
        }

        public DigitalGoodsFeedback(long purchaseId) {
            this.purchaseId = purchaseId;
        }

        @Override
        protected int getMySize() {
            return 8;
        }

        @Override
        protected void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(purchaseId);
        }

        @Override
        protected void putMyJSON(JSONObject attachment) {
            attachment.put("purchase", Long.toUnsignedString(purchaseId));
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.FEEDBACK;
        }

        public long getPurchaseId() { return purchaseId; }

    }

    final class DigitalGoodsRefund extends AbstractAttachment {

        private final long purchaseId;
        private final long refundNQT;

        DigitalGoodsRefund(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.purchaseId = buffer.getLong();
            this.refundNQT = buffer.getLong();
        }

        DigitalGoodsRefund(JSONObject attachmentData) {
            super(attachmentData);
            this.purchaseId = Convert.parseUnsignedLong((String)attachmentData.get("purchase"));
            this.refundNQT = Convert.parseLong(attachmentData.get("refundNQT"));
        }

        public DigitalGoodsRefund(long purchaseId, long refundNQT) {
            this.purchaseId = purchaseId;
            this.refundNQT = refundNQT;
        }

        @Override
        protected int getMySize() {
            return 8 + 8;
        }

        @Override
        protected void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(purchaseId);
            buffer.putLong(refundNQT);
        }

        @Override
        protected void putMyJSON(JSONObject attachment) {
            attachment.put("purchase", Long.toUnsignedString(purchaseId));
            attachment.put("refundNQT", refundNQT);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.REFUND;
        }

        public long getPurchaseId() { return purchaseId; }

        public long getRefundNQT() { return refundNQT; }

    }

    interface MonetarySystemAttachment {

        long getCurrencyId();

    }

    final class MonetarySystemCurrencyIssuance extends AbstractAttachment {

        private final String name;
        private final String code;
        private final String description;
        private final byte type;
        private final long initialSupply;
        private final long reserveSupply;
        private final long maxSupply;
        private final int issuanceHeight;
        private final long minReservePerUnitNQT;
        private final int minDifficulty;
        private final int maxDifficulty;
        private final byte ruleset;
        private final byte algorithm;
        private final byte decimals;

        MonetarySystemCurrencyIssuance(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            super(buffer, transactionVersion);
            this.name = Convert.readString(buffer, buffer.get(), Constants.MAX_CURRENCY_NAME_LENGTH);
            this.code = Convert.readString(buffer, buffer.get(), Constants.MAX_CURRENCY_CODE_LENGTH);
            this.description = Convert.readString(buffer, buffer.getShort(), Constants.MAX_CURRENCY_DESCRIPTION_LENGTH);
            this.type = buffer.get();
            this.initialSupply = buffer.getLong();
            this.reserveSupply = buffer.getLong();
            this.maxSupply = buffer.getLong();
            this.issuanceHeight = buffer.getInt();
            this.minReservePerUnitNQT = buffer.getLong();
            this.minDifficulty = buffer.get() & 0xFF;
            this.maxDifficulty = buffer.get() & 0xFF;
            this.ruleset = buffer.get();
            this.algorithm = buffer.get();
            this.decimals = buffer.get();
        }

        MonetarySystemCurrencyIssuance(JSONObject attachmentData) {
            super(attachmentData);
            this.name = (String)attachmentData.get("name");
            this.code = (String)attachmentData.get("code");
            this.description = (String)attachmentData.get("description");
            this.type = ((Long)attachmentData.get("type")).byteValue();
            this.initialSupply = Convert.parseLong(attachmentData.get("initialSupply"));
            this.reserveSupply = Convert.parseLong(attachmentData.get("reserveSupply"));
            this.maxSupply = Convert.parseLong(attachmentData.get("maxSupply"));
            this.issuanceHeight = ((Long)attachmentData.get("issuanceHeight")).intValue();
            this.minReservePerUnitNQT = Convert.parseLong(attachmentData.get("minReservePerUnitNQT"));
            this.minDifficulty = ((Long)attachmentData.get("minDifficulty")).intValue();
            this.maxDifficulty = ((Long)attachmentData.get("maxDifficulty")).intValue();
            this.ruleset = ((Long)attachmentData.get("ruleset")).byteValue();
            this.algorithm = ((Long)attachmentData.get("algorithm")).byteValue();
            this.decimals = ((Long) attachmentData.get("decimals")).byteValue();
        }

        public MonetarySystemCurrencyIssuance(String name, String code, String description, byte type, long initialSupply, long reserveSupply,
                                              long maxSupply, int issuanceHeight, long minReservePerUnitNQT, int minDifficulty, int maxDifficulty,
                                              byte ruleset, byte algorithm, byte decimals) {
            this.name = name;
            this.code = code;
            this.description = description;
            this.type = type;
            this.initialSupply = initialSupply;
            this.reserveSupply = reserveSupply;
            this.maxSupply = maxSupply;
            this.issuanceHeight = issuanceHeight;
            this.minReservePerUnitNQT = minReservePerUnitNQT;
            this.minDifficulty = minDifficulty;
            this.maxDifficulty = maxDifficulty;
            this.ruleset = ruleset;
            this.algorithm = algorithm;
            this.decimals = decimals;
        }

        @Override
        protected int getMySize() {
            return 1 + Convert.toBytes(name).length + 1 + Convert.toBytes(code).length + 2 +
                    Convert.toBytes(description).length + 1 + 8 + 8 + 8 + 4 + 8 + 1 + 1 + 1 + 1 + 1;
        }

        @Override
        protected void putMyBytes(ByteBuffer buffer) {
            byte[] name = Convert.toBytes(this.name);
            byte[] code = Convert.toBytes(this.code);
            byte[] description = Convert.toBytes(this.description);
            buffer.put((byte)name.length);
            buffer.put(name);
            buffer.put((byte)code.length);
            buffer.put(code);
            buffer.putShort((short) description.length);
            buffer.put(description);
            buffer.put(type);
            buffer.putLong(initialSupply);
            buffer.putLong(reserveSupply);
            buffer.putLong(maxSupply);
            buffer.putInt(issuanceHeight);
            buffer.putLong(minReservePerUnitNQT);
            buffer.put((byte)minDifficulty);
            buffer.put((byte)maxDifficulty);
            buffer.put(ruleset);
            buffer.put(algorithm);
            buffer.put(decimals);
        }

        @Override
        protected void putMyJSON(JSONObject attachment) {
            attachment.put("name", name);
            attachment.put("code", code);
            attachment.put("description", description);
            attachment.put("type", type);
            attachment.put("initialSupply", initialSupply);
            attachment.put("reserveSupply", reserveSupply);
            attachment.put("maxSupply", maxSupply);
            attachment.put("issuanceHeight", issuanceHeight);
            attachment.put("minReservePerUnitNQT", minReservePerUnitNQT);
            attachment.put("minDifficulty", minDifficulty);
            attachment.put("maxDifficulty", maxDifficulty);
            attachment.put("ruleset", ruleset);
            attachment.put("algorithm", algorithm);
            attachment.put("decimals", decimals);
        }

        @Override
        public TransactionType getTransactionType() {
            return MonetarySystem.CURRENCY_ISSUANCE;
        }

        public String getName() {
            return name;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public byte getType() {
            return type;
        }

        public long getInitialSupply() {
            return initialSupply;
        }

        public long getReserveSupply() {
            return reserveSupply;
        }

        public long getMaxSupply() {
            return maxSupply;
        }

        public int getIssuanceHeight() {
            return issuanceHeight;
        }

        public long getMinReservePerUnitNQT() {
            return minReservePerUnitNQT;
        }

        public int getMinDifficulty() {
            return minDifficulty;
        }

        public int getMaxDifficulty() {
            return maxDifficulty;
        }

        public byte getRuleset() {
            return ruleset;
        }

        public byte getAlgorithm() {
            return algorithm;
        }

        public byte getDecimals() {
            return decimals;
        }
    }

    final class MonetarySystemReserveIncrease extends AbstractAttachment implements MonetarySystemAttachment {

        private final long currencyId;
        private final long amountPerUnitNQT;

        MonetarySystemReserveIncrease(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.currencyId = buffer.getLong();
            this.amountPerUnitNQT = buffer.getLong();
        }

        MonetarySystemReserveIncrease(JSONObject attachmentData) {
            super(attachmentData);
            this.currencyId = Convert.parseUnsignedLong((String)attachmentData.get("currency"));
            this.amountPerUnitNQT = Convert.parseLong(attachmentData.get("amountPerUnitNQT"));
        }

        public MonetarySystemReserveIncrease(long currencyId, long amountPerUnitNQT) {
            this.currencyId = currencyId;
            this.amountPerUnitNQT = amountPerUnitNQT;
        }

        @Override
        protected int getMySize() {
            return 8 + 8;
        }

        @Override
        protected void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(currencyId);
            buffer.putLong(amountPerUnitNQT);
        }

        @Override
        protected void putMyJSON(JSONObject attachment) {
            attachment.put("currency", Long.toUnsignedString(currencyId));
            attachment.put("amountPerUnitNQT", amountPerUnitNQT);
        }

        @Override
        public TransactionType getTransactionType() {
            return MonetarySystem.RESERVE_INCREASE;
        }

        @Override
        public long getCurrencyId() {
            return currencyId;
        }

        public long getAmountPerUnitNQT() {
            return amountPerUnitNQT;
        }

    }

    final class MonetarySystemReserveClaim extends AbstractAttachment implements MonetarySystemAttachment {

        private final long currencyId;
        private final long units;

        MonetarySystemReserveClaim(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.currencyId = buffer.getLong();
            this.units = buffer.getLong();
        }

        MonetarySystemReserveClaim(JSONObject attachmentData) {
            super(attachmentData);
            this.currencyId = Convert.parseUnsignedLong((String)attachmentData.get("currency"));
            this.units = Convert.parseLong(attachmentData.get("units"));
        }

        public MonetarySystemReserveClaim(long currencyId, long units) {
            this.currencyId = currencyId;
            this.units = units;
        }

        @Override
        protected int getMySize() {
            return 8 + 8;
        }

        @Override
        protected void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(currencyId);
            buffer.putLong(units);
        }

        @Override
        protected void putMyJSON(JSONObject attachment) {
            attachment.put("currency", Long.toUnsignedString(currencyId));
            attachment.put("units", units);
        }

        @Override
        public TransactionType getTransactionType() {
            return MonetarySystem.RESERVE_CLAIM;
        }

        @Override
        public long getCurrencyId() {
            return currencyId;
        }

        public long getUnits() {
            return units;
        }

    }

    final class MonetarySystemCurrencyTransfer extends AbstractAttachment implements MonetarySystemAttachment {

        private final long currencyId;
        private final long units;

        MonetarySystemCurrencyTransfer(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.currencyId = buffer.getLong();
            this.units = buffer.getLong();
        }

        MonetarySystemCurrencyTransfer(JSONObject attachmentData) {
            super(attachmentData);
            this.currencyId = Convert.parseUnsignedLong((String)attachmentData.get("currency"));
            this.units = Convert.parseLong(attachmentData.get("units"));
        }

        public MonetarySystemCurrencyTransfer(long currencyId, long units) {
            this.currencyId = currencyId;
            this.units = units;
        }

        @Override
        protected int getMySize() {
            return 8 + 8;
        }

        @Override
        protected void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(currencyId);
            buffer.putLong(units);
        }

        @Override
        protected void putMyJSON(JSONObject attachment) {
            attachment.put("currency", Long.toUnsignedString(currencyId));
            attachment.put("units", units);
        }

        @Override
        public TransactionType getTransactionType() {
            return MonetarySystem.CURRENCY_TRANSFER;
        }

        @Override
        public long getCurrencyId() {
            return currencyId;
        }

        public long getUnits() {
            return units;
        }
    }

    final class MonetarySystemPublishExchangeOffer extends AbstractAttachment implements MonetarySystemAttachment {

        private final long currencyId;
        private final long buyRateNQT;
        private final long sellRateNQT;
        private final long totalBuyLimit;
        private final long totalSellLimit;
        private final long initialBuySupply;
        private final long initialSellSupply;
        private final int expirationHeight;

        MonetarySystemPublishExchangeOffer(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.currencyId = buffer.getLong();
            this.buyRateNQT = buffer.getLong();
            this.sellRateNQT = buffer.getLong();
            this.totalBuyLimit = buffer.getLong();
            this.totalSellLimit = buffer.getLong();
            this.initialBuySupply = buffer.getLong();
            this.initialSellSupply = buffer.getLong();
            this.expirationHeight = buffer.getInt();
        }

        MonetarySystemPublishExchangeOffer(JSONObject attachmentData) {
            super(attachmentData);
            this.currencyId = Convert.parseUnsignedLong((String)attachmentData.get("currency"));
            this.buyRateNQT = Convert.parseLong(attachmentData.get("buyRateNQT"));
            this.sellRateNQT = Convert.parseLong(attachmentData.get("sellRateNQT"));
            this.totalBuyLimit = Convert.parseLong(attachmentData.get("totalBuyLimit"));
            this.totalSellLimit = Convert.parseLong(attachmentData.get("totalSellLimit"));
            this.initialBuySupply = Convert.parseLong(attachmentData.get("initialBuySupply"));
            this.initialSellSupply = Convert.parseLong(attachmentData.get("initialSellSupply"));
            this.expirationHeight = ((Long)attachmentData.get("expirationHeight")).intValue();
        }

        public MonetarySystemPublishExchangeOffer(long currencyId, long buyRateNQT, long sellRateNQT, long totalBuyLimit,
                                                  long totalSellLimit, long initialBuySupply, long initialSellSupply, int expirationHeight) {
            this.currencyId = currencyId;
            this.buyRateNQT = buyRateNQT;
            this.sellRateNQT = sellRateNQT;
            this.totalBuyLimit = totalBuyLimit;
            this.totalSellLimit = totalSellLimit;
            this.initialBuySupply = initialBuySupply;
            this.initialSellSupply = initialSellSupply;
            this.expirationHeight = expirationHeight;
        }

        @Override
        protected int getMySize() {
            return 8 + 8 + 8 + 8 + 8 + 8 + 8 + 4;
        }

        @Override
        protected void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(currencyId);
            buffer.putLong(buyRateNQT);
            buffer.putLong(sellRateNQT);
            buffer.putLong(totalBuyLimit);
            buffer.putLong(totalSellLimit);
            buffer.putLong(initialBuySupply);
            buffer.putLong(initialSellSupply);
            buffer.putInt(expirationHeight);
        }

        @Override
        protected void putMyJSON(JSONObject attachment) {
            attachment.put("currency", Long.toUnsignedString(currencyId));
            attachment.put("buyRateNQT", buyRateNQT);
            attachment.put("sellRateNQT", sellRateNQT);
            attachment.put("totalBuyLimit", totalBuyLimit);
            attachment.put("totalSellLimit", totalSellLimit);
            attachment.put("initialBuySupply", initialBuySupply);
            attachment.put("initialSellSupply", initialSellSupply);
            attachment.put("expirationHeight", expirationHeight);
        }

        @Override
        public TransactionType getTransactionType() {
            return MonetarySystem.PUBLISH_EXCHANGE_OFFER;
        }

        @Override
        public long getCurrencyId() {
            return currencyId;
        }

        public long getBuyRateNQT() {
            return buyRateNQT;
        }

        public long getSellRateNQT() {
            return sellRateNQT;
        }

        public long getTotalBuyLimit() {
            return totalBuyLimit;
        }

        public long getTotalSellLimit() {
            return totalSellLimit;
        }

        public long getInitialBuySupply() {
            return initialBuySupply;
        }

        public long getInitialSellSupply() {
            return initialSellSupply;
        }

        public int getExpirationHeight() {
            return expirationHeight;
        }

    }

    abstract class MonetarySystemExchange extends AbstractAttachment implements MonetarySystemAttachment {

        private final long currencyId;
        private final long rateNQT;
        private final long units;

        private MonetarySystemExchange(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.currencyId = buffer.getLong();
            this.rateNQT = buffer.getLong();
            this.units = buffer.getLong();
        }

        private MonetarySystemExchange(JSONObject attachmentData) {
            super(attachmentData);
            this.currencyId = Convert.parseUnsignedLong((String)attachmentData.get("currency"));
            this.rateNQT = Convert.parseLong(attachmentData.get("rateNQT"));
            this.units = Convert.parseLong(attachmentData.get("units"));
        }

        private MonetarySystemExchange(long currencyId, long rateNQT, long units) {
            this.currencyId = currencyId;
            this.rateNQT = rateNQT;
            this.units = units;
        }

        @Override
        protected int getMySize() {
            return 8 + 8 + 8;
        }

        @Override
        protected void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(currencyId);
            buffer.putLong(rateNQT);
            buffer.putLong(units);
        }

        @Override
        protected void putMyJSON(JSONObject attachment) {
            attachment.put("currency", Long.toUnsignedString(currencyId));
            attachment.put("rateNQT", rateNQT);
            attachment.put("units", units);
        }

        @Override
        public long getCurrencyId() {
            return currencyId;
        }

        public long getRateNQT() {
            return rateNQT;
        }

        public long getUnits() {
            return units;
        }

    }

    final class MonetarySystemExchangeBuy extends MonetarySystemExchange {

        MonetarySystemExchangeBuy(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
        }

        MonetarySystemExchangeBuy(JSONObject attachmentData) {
            super(attachmentData);
        }

        public MonetarySystemExchangeBuy(long currencyId, long rateNQT, long units) {
            super(currencyId, rateNQT, units);
        }

        @Override
        public TransactionType getTransactionType() {
            return MonetarySystem.EXCHANGE_BUY;
        }

    }

    final class MonetarySystemExchangeSell extends MonetarySystemExchange {

        MonetarySystemExchangeSell(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
        }

        MonetarySystemExchangeSell(JSONObject attachmentData) {
            super(attachmentData);
        }

        public MonetarySystemExchangeSell(long currencyId, long rateNQT, long units) {
            super(currencyId, rateNQT, units);
        }

        @Override
        public TransactionType getTransactionType() {
            return MonetarySystem.EXCHANGE_SELL;
        }

    }

    final class MonetarySystemCurrencyMinting extends AbstractAttachment implements MonetarySystemAttachment {

        private final long nonce;
        private final long currencyId;
        private final long units;
        private final long counter;

        MonetarySystemCurrencyMinting(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.nonce = buffer.getLong();
            this.currencyId = buffer.getLong();
            this.units = buffer.getLong();
            this.counter = buffer.getLong();
        }

        MonetarySystemCurrencyMinting(JSONObject attachmentData) {
            super(attachmentData);
            this.nonce = Convert.parseLong(attachmentData.get("nonce"));
            this.currencyId = Convert.parseUnsignedLong((String)attachmentData.get("currency"));
            this.units = Convert.parseLong(attachmentData.get("units"));
            this.counter = Convert.parseLong(attachmentData.get("counter"));
        }

        public MonetarySystemCurrencyMinting(long nonce, long currencyId, long units, long counter) {
            this.nonce = nonce;
            this.currencyId = currencyId;
            this.units = units;
            this.counter = counter;
        }

        @Override
        protected int getMySize() {
            return 8 + 8 + 8 + 8;
        }

        @Override
        protected void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(nonce);
            buffer.putLong(currencyId);
            buffer.putLong(units);
            buffer.putLong(counter);
        }

        @Override
        protected void putMyJSON(JSONObject attachment) {
            attachment.put("nonce", nonce);
            attachment.put("currency", Long.toUnsignedString(currencyId));
            attachment.put("units", units);
            attachment.put("counter", counter);
        }

        @Override
        public TransactionType getTransactionType() {
            return MonetarySystem.CURRENCY_MINTING;
        }

        public long getNonce() {
            return nonce;
        }

        @Override
        public long getCurrencyId() {
            return currencyId;
        }

        public long getUnits() {
            return units;
        }

        public long getCounter() {
            return counter;
        }

    }

    final class MonetarySystemCurrencyDeletion extends AbstractAttachment implements MonetarySystemAttachment {

        private final long currencyId;

        MonetarySystemCurrencyDeletion(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.currencyId = buffer.getLong();
        }

        MonetarySystemCurrencyDeletion(JSONObject attachmentData) {
            super(attachmentData);
            this.currencyId = Convert.parseUnsignedLong((String)attachmentData.get("currency"));
        }

        public MonetarySystemCurrencyDeletion(long currencyId) {
            this.currencyId = currencyId;
        }

        @Override
        protected int getMySize() {
            return 8;
        }

        @Override
        protected void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(currencyId);
        }

        @Override
        protected void putMyJSON(JSONObject attachment) {
            attachment.put("currency", Long.toUnsignedString(currencyId));
        }

        @Override
        public TransactionType getTransactionType() {
            return MonetarySystem.CURRENCY_DELETION;
        }

        @Override
        public long getCurrencyId() {
            return currencyId;
        }

    }

    abstract class TaggedDataAttachment extends AbstractAttachment implements Prunable {

        private final String name;
        private final String description;
        private final String tags;
        private final String type;
        private final String channel;
        private final boolean isText;
        private final String filename;
        private final byte[] data;
        private volatile TaggedData taggedData;

        private TaggedDataAttachment(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.name = null;
            this.description = null;
            this.tags = null;
            this.type = null;
            this.channel = null;
            this.isText = false;
            this.filename = null;
            this.data = null;
        }

        private TaggedDataAttachment(JSONObject attachmentData) {
            String dataJSON = (String) attachmentData.get("data");
            if (dataJSON != null) {
                this.name = (String) attachmentData.get("name");
                this.description = (String) attachmentData.get("description");
                this.tags = (String) attachmentData.get("tags");
                this.type = (String) attachmentData.get("type");
                this.channel = Convert.nullToEmpty((String) attachmentData.get("channel"));
                this.isText = Boolean.TRUE.equals(attachmentData.get("isText"));
                this.data = isText ? Convert.toBytes(dataJSON) : Convert.parseHexString(dataJSON);
                this.filename = (String) attachmentData.get("filename");
            } else {
                this.name = null;
                this.description = null;
                this.tags = null;
                this.type = null;
                this.channel = null;
                this.isText = false;
                this.filename = null;
                this.data = null;
            }

        }

        private TaggedDataAttachment(String name, String description, String tags, String type, String channel, boolean isText, String filename, byte[] data) {
            this.name = name;
            this.description = description;
            this.tags = tags;
            this.type = type;
            this.channel = channel;
            this.isText = isText;
            this.data = data;
            this.filename = filename;
        }

        @Override
        final int getMyFullSize() {
            if (getData() == null) {
                return 0;
            }
            return Convert.toBytes(getName()).length + Convert.toBytes(getDescription()).length + Convert.toBytes(getType()).length
                    + Convert.toBytes(getChannel()).length + Convert.toBytes(getTags()).length + Convert.toBytes(getFilename()).length + getData().length;
        }

        @Override
        protected void putMyJSON(JSONObject attachment) {
            if (taggedData != null) {
                attachment.put("name", taggedData.getName());
                attachment.put("description", taggedData.getDescription());
                attachment.put("tags", taggedData.getTags());
                attachment.put("type", taggedData.getType());
                attachment.put("channel", taggedData.getChannel());
                attachment.put("isText", taggedData.isText());
                attachment.put("filename", taggedData.getFilename());
                attachment.put("data", taggedData.isText() ? Convert.toString(taggedData.getData()) : Convert.toHexString(taggedData.getData()));
            } else if (data != null) {
                attachment.put("name", name);
                attachment.put("description", description);
                attachment.put("tags", tags);
                attachment.put("type", type);
                attachment.put("channel", channel);
                attachment.put("isText", isText);
                attachment.put("filename", filename);
                attachment.put("data", isText ? Convert.toString(data) : Convert.toHexString(data));
            }
        }

        @Override
        public byte[] getHash() {
            if (data == null) {
                return null;
            }
            MessageDigest digest = Crypto.sha256();
            digest.update(Convert.toBytes(name));
            digest.update(Convert.toBytes(description));
            digest.update(Convert.toBytes(tags));
            digest.update(Convert.toBytes(type));
            digest.update(Convert.toBytes(channel));
            digest.update((byte)(isText ? 1 : 0));
            digest.update(Convert.toBytes(filename));
            digest.update(data);
            return digest.digest();
        }

        public final String getName() {
            if (taggedData != null) {
                return taggedData.getName();
            }
            return name;
        }

        public final String getDescription() {
            if (taggedData != null) {
                return taggedData.getDescription();
            }
            return description;
        }

        public final String getTags() {
            if (taggedData != null) {
                return taggedData.getTags();
            }
            return tags;
        }

        public final String getType() {
            if (taggedData != null) {
                return taggedData.getType();
            }
            return type;
        }

        public final String getChannel() {
            if (taggedData != null) {
                return taggedData.getChannel();
            }
            return channel;
        }

        public final boolean isText() {
            if (taggedData != null) {
                return taggedData.isText();
            }
            return isText;
        }

        public final String getFilename() {
            if (taggedData != null) {
                return taggedData.getFilename();
            }
            return filename;
        }

        public final byte[] getData() {
            if (taggedData != null) {
                return taggedData.getData();
            }
            return data;
        }

        @Override
        void loadPrunable(Transaction transaction, boolean includeExpiredPrunable) {
            if (data == null && taggedData == null && shouldLoadPrunable(transaction, includeExpiredPrunable)) {
                taggedData = TaggedData.getData(getTaggedDataId(transaction));
            }
        }

        abstract long getTaggedDataId(Transaction transaction);

    }

    final class TaggedDataUpload extends TaggedDataAttachment {

        static TaggedDataUpload parse(JSONObject attachmentData) {
            if (!Appendix.hasAppendix(TransactionType.Data.TAGGED_DATA_UPLOAD.getName(), attachmentData)) {
                return null;
            }
            return new TaggedDataUpload(attachmentData);
        }

        private final byte[] hash;

        TaggedDataUpload(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.hash = new byte[32];
            buffer.get(hash);
        }

        TaggedDataUpload(JSONObject attachmentData) {
            super(attachmentData);
            String dataJSON = (String) attachmentData.get("data");
            if (dataJSON == null) {
                this.hash = Convert.parseHexString(Convert.emptyToNull((String)attachmentData.get("hash")));
            } else {
                this.hash = null;
            }
        }

        public TaggedDataUpload(String name, String description, String tags, String type, String channel, boolean isText,
                                String filename, byte[] data) throws NxtException.NotValidException {
            super(name, description, tags, type, channel, isText, filename, data);
            this.hash = null;
            if (isText && !Arrays.equals(data, Convert.toBytes(Convert.toString(data)))) {
                throw new NxtException.NotValidException("Data is not UTF-8 text");
            }
        }

        @Override
        protected int getMySize() {
            return 32;
        }

        @Override
        protected void putMyBytes(ByteBuffer buffer) {
            buffer.put(getHash());
        }

        @Override
        protected void putMyJSON(JSONObject attachment) {
            super.putMyJSON(attachment);
            attachment.put("hash", Convert.toHexString(getHash()));
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Data.TAGGED_DATA_UPLOAD;
        }

        @Override
        public byte[] getHash() {
            if (hash != null) {
                return hash;
            }
            return super.getHash();
        }

        @Override
        long getTaggedDataId(Transaction transaction) {
            return transaction.getId();
        }

    }

    final class TaggedDataExtend extends TaggedDataAttachment {

        static TaggedDataExtend parse(JSONObject attachmentData) {
            if (!Appendix.hasAppendix(TransactionType.Data.TAGGED_DATA_EXTEND.getName(), attachmentData)) {
                return null;
            }
            return new TaggedDataExtend(attachmentData);
        }

        private volatile byte[] hash;
        private final long taggedDataId;
        private final boolean jsonIsPruned;

        TaggedDataExtend(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.taggedDataId = buffer.getLong();
            this.jsonIsPruned = false;
        }

        TaggedDataExtend(JSONObject attachmentData) {
            super(attachmentData);
            this.taggedDataId = Convert.parseUnsignedLong((String)attachmentData.get("taggedData"));
            this.jsonIsPruned = attachmentData.get("data") == null;
        }

        public TaggedDataExtend(TaggedData taggedData) {
            super(taggedData.getName(), taggedData.getDescription(), taggedData.getTags(), taggedData.getType(),
                    taggedData.getChannel(), taggedData.isText(), taggedData.getFilename(), taggedData.getData());
            this.taggedDataId = taggedData.getId();
            this.jsonIsPruned = false;
        }

        @Override
        protected int getMySize() {
            return 8;
        }

        @Override
        protected void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(taggedDataId);
        }

        @Override
        protected void putMyJSON(JSONObject attachment) {
            super.putMyJSON(attachment);
            attachment.put("taggedData", Long.toUnsignedString(taggedDataId));
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Data.TAGGED_DATA_EXTEND;
        }

        public long getTaggedDataId() {
            return taggedDataId;
        }

        @Override
        public byte[] getHash() {
            if (hash == null) {
                hash = super.getHash();
            }
            if (hash == null) {
                TaggedDataUpload taggedDataUpload = (TaggedDataUpload)TransactionDb.findTransaction(taggedDataId).getAttachment();
                hash = taggedDataUpload.getHash();
            }
            return hash;
        }

        @Override
        long getTaggedDataId(Transaction transaction) {
            return taggedDataId;
        }

        boolean jsonIsPruned() {
            return jsonIsPruned;
        }

    }

}
