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

package nxt.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import nxt.Account;
import nxt.Attachment;
import nxt.Attachment.MessagingPollCreation.PollBuilder;
import nxt.Constants;
import nxt.Nxt;
import nxt.NxtException;
import nxt.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.util.ArrayList;
import java.util.List;

import static nxt.http.JSONResponses.INCORRECT_POLL_DESCRIPTION_LENGTH;
import static nxt.http.JSONResponses.INCORRECT_POLL_NAME_LENGTH;
import static nxt.http.JSONResponses.INCORRECT_POLL_OPTION_LENGTH;
import static nxt.http.JSONResponses.INCORRECT_ZEROOPTIONS;
import static nxt.http.JSONResponses.MISSING_DESCRIPTION;
import static nxt.http.JSONResponses.MISSING_NAME;

@Path("/fimk?requestType=createPoll")
public final class CreatePoll extends CreateTransaction {

    static final CreatePoll instance = new CreatePoll();

    private CreatePoll() {
        super(new APITag[]{APITag.VS, APITag.CREATE_TRANSACTION},
                "name", "description", "finishHeight", "votingModel",
                "minNumberOfOptions", "maxNumberOfOptions",
                "minRangeValue", "maxRangeValue",
                "minBalance", "minBalanceModel", "holding",
                "option00", "option01", "option02");
    }

    @Override
    @POST
    @Operation(summary = "Create poll",
            tags = {APITag2.VS, APITag2.TRANSACTIONS})
    @Parameter(name = "name", in = ParameterIn.QUERY, required = true)
    @Parameter(name = "description", in = ParameterIn.QUERY, required = true)
    @Parameter(name = "finishHeight", in = ParameterIn.QUERY, required = true, schema = @Schema(type = "integer"),
            description = "finish height")
    @Parameter(name = "votingModel", in = ParameterIn.QUERY, required = true, schema = @Schema(type = "integer"),
            description = "voting model")
    @Parameter(name = "minNumberOfOptions", in = ParameterIn.QUERY, required = true, schema = @Schema(type = "integer"),
            description = "min number of options")
    @Parameter(name = "maxNumberOfOptions", in = ParameterIn.QUERY, required = true, schema = @Schema(type = "integer"),
            description = "max number of options")
    @Parameter(name = "minRangeValue", in = ParameterIn.QUERY, required = true, schema = @Schema(type = "integer"),
            description = "min range value")
    @Parameter(name = "maxRangeValue", in = ParameterIn.QUERY, required = true, schema = @Schema(type = "integer"),
            description = "max range value")
    @Parameter(name = "minBalance", in = ParameterIn.QUERY, schema = @Schema(type = "integer"),
            description = "min balance")
    @Parameter(name = "minBalanceModel", in = ParameterIn.QUERY, required = true, schema = @Schema(type = "integer"),
            description = "min balance model")
    @Parameter(name = "holding", in = ParameterIn.QUERY,
            description = "holding id")
    @Parameter(name = "option00", in = ParameterIn.QUERY,
            description = "option value #0")
    @Parameter(name = "option01", in = ParameterIn.QUERY,
            description = "option value #1")
    @Parameter(name = "option02", in = ParameterIn.QUERY,
            description = "option value #2")


    @Parameter(name = "aliasName", in = ParameterIn.QUERY, required = true, description = "alias name")
    @Parameter(name = "amountNQT", in = ParameterIn.QUERY, required = true, description = "amount in NQT")
    public JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        String nameValue = Convert.emptyToNull(req.getParameter("name"));
        String descriptionValue = req.getParameter("description");

        if (nameValue == null || nameValue.trim().isEmpty()) {
            return MISSING_NAME;
        } else if (descriptionValue == null) {
            return MISSING_DESCRIPTION;
        }

        if (nameValue.length() > Constants.MAX_POLL_NAME_LENGTH) {
            return INCORRECT_POLL_NAME_LENGTH;
        }

        if (descriptionValue.length() > Constants.MAX_POLL_DESCRIPTION_LENGTH) {
            return INCORRECT_POLL_DESCRIPTION_LENGTH;
        }

        List<String> options = new ArrayList<>();
        while (options.size() < Constants.MAX_POLL_OPTION_COUNT) {
            int i = options.size();
            String optionValue = Convert.emptyToNull(req.getParameter("option" + (i < 10 ? "0" + i : i)));
            if (optionValue == null) {
                break;
            }
            if (optionValue.length() > Constants.MAX_POLL_OPTION_LENGTH || (optionValue = optionValue.trim()).isEmpty()) {
                return INCORRECT_POLL_OPTION_LENGTH;
            }
            options.add(optionValue);
        }

        byte optionsSize = (byte) options.size();
        if (options.size() == 0) {
            return INCORRECT_ZEROOPTIONS;
        }

        int currentHeight = Nxt.getBlockchain().getHeight();
        int finishHeight = ParameterParser.getInt(req, "finishHeight",
                currentHeight + 2,
                currentHeight + Constants.MAX_POLL_DURATION + 1, true);

        byte votingModel = ParameterParser.getByte(req, "votingModel", (byte)0, (byte)3, true);

        byte minNumberOfOptions = ParameterParser.getByte(req, "minNumberOfOptions", (byte) 1, optionsSize, true);
        byte maxNumberOfOptions = ParameterParser.getByte(req, "maxNumberOfOptions", minNumberOfOptions, optionsSize, true);

        byte minRangeValue = ParameterParser.getByte(req, "minRangeValue", Constants.MIN_VOTE_VALUE, Constants.MAX_VOTE_VALUE, true);
        byte maxRangeValue = ParameterParser.getByte(req, "maxRangeValue", minRangeValue, Constants.MAX_VOTE_VALUE, true);

        PollBuilder builder = new PollBuilder(nameValue.trim(), descriptionValue.trim(),
                options.toArray(new String[options.size()]), finishHeight, votingModel,
                minNumberOfOptions, maxNumberOfOptions, minRangeValue, maxRangeValue);

        long minBalance = ParameterParser.getLong(req, "minBalance", 0, Long.MAX_VALUE, false);

        if (minBalance != 0) {
            byte minBalanceModel = ParameterParser.getByte(req, "minBalanceModel", (byte)0, (byte)3, true);
            builder.minBalance(minBalanceModel, minBalance);
        }

        long holdingId = ParameterParser.getUnsignedLong(req, "holding", false);
        if (holdingId != 0) {
            builder.holdingId(holdingId);
        }

        Account account = ParameterParser.getSenderAccount(req);
        Attachment attachment = builder.build();
        return createTransaction(req, account, attachment);
    }
}
