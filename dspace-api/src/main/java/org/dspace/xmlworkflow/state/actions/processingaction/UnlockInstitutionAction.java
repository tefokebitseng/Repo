/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.state.actions.processingaction;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.dspace.authority.service.AuthorityValueService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.service.EntityService;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.InstallItemService;
import org.dspace.core.Context;
import org.dspace.core.Context.Mode;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.DiscoverResultIterator;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.versioning.ItemCorrectionService;
import org.dspace.workflow.WorkflowException;
import org.dspace.xmlworkflow.ConcytecFeedback;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.xmlworkflow.factory.XmlWorkflowFactory;
import org.dspace.xmlworkflow.service.ConcytecWorkflowService;
import org.dspace.xmlworkflow.service.XmlWorkflowService;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.Workflow;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.state.actions.WorkflowActionConfig;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Action to unlock the institution workflow.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class UnlockInstitutionAction extends ProcessingAction {

    @Autowired
    private XmlWorkflowItemService workflowItemService;

    @Autowired
    private XmlWorkflowFactory workflowFactory;

    @Autowired
    private XmlWorkflowService workflowService;

    @Autowired
    private ConcytecWorkflowService concytecWorkflowService;

    @Autowired
    private InstallItemService installItemService;

    @Autowired
    private ItemCorrectionService itemCorrectionService;

    @Autowired
    private ChoiceAuthorityService choiceAuthorityService;

    @Override
    public void activate(Context c, XmlWorkflowItem wf) {
    }

    @Override
    public ActionResult execute(Context context, XmlWorkflowItem workflowItem, Step step, HttpServletRequest request)
        throws SQLException, AuthorizeException, IOException, WorkflowException {

        Item item = workflowItem.getItem();

        Item institutionItem = concytecWorkflowService.findCopiedItem(context, item);

        Item itemToCorrect = itemCorrectionService.getCorrectedItem(context, item);
        boolean isCorrectionItem = itemToCorrect != null;

        ConcytecFeedback concytecFeedback = concytecWorkflowService.getConcytecFeedback(context, item);

        try {

            if (institutionItem != null) {
                unlockInstitutionWorkflow(context, item, institutionItem, concytecFeedback);
            }

        } catch (WorkflowConfigurationException e) {
            throw new WorkflowException(e);
        }

        if (isCorrectionItem) {
            return finalizeItemCorrection(context, workflowItem, concytecFeedback);
        } else {
            return finalizeItemCreation(context, workflowItem, institutionItem, concytecFeedback);
        }

    }

    private void unlockInstitutionWorkflow(Context context, Item directorioItem, Item institutionItem,
        ConcytecFeedback concytecFeedback) throws IOException, AuthorizeException, SQLException, WorkflowException,
        WorkflowConfigurationException {

        XmlWorkflowItem institutionWorkflowItem = workflowItemService.findByItem(context, institutionItem);

        if (concytecFeedback != ConcytecFeedback.NONE) {
            concytecWorkflowService.setConcytecFeedback(context, institutionItem, concytecFeedback);
            String concytecComment = concytecWorkflowService.getConcytecComment(context, directorioItem);
            if (StringUtils.isNotBlank(concytecComment)) {
                concytecWorkflowService.setConcytecComment(context, institutionItem, concytecComment);
            }
        }

        Workflow institutionWorkflow = workflowFactory.getWorkflow(institutionWorkflowItem.getCollection());
        Step waitForConcytecStep = institutionWorkflow.getStep("waitForConcytecStep");
        WorkflowActionConfig waitForConcytecActionConfig = waitForConcytecStep.getActionConfig("waitForConcytecAction");

        workflowService.processOutcome(context, context.getCurrentUser(), institutionWorkflow, waitForConcytecStep,
            waitForConcytecActionConfig, getCompleteActionResult(), institutionWorkflowItem, true);

    }

    private ActionResult finalizeItemCorrection(Context context, XmlWorkflowItem workflowItem,
        ConcytecFeedback concytecFeedback) throws SQLException, AuthorizeException, IOException {

        if (concytecFeedback == ConcytecFeedback.REJECT) {
            workflowService.deleteWorkflowByWorkflowItem(context, workflowItem, context.getCurrentUser());
            return getCancelActionResult();
        } else {
            itemCorrectionService.replaceCorrectionItemWithNative(context, workflowItem);
            return getCompleteActionResult();
        }

    }

    private ActionResult finalizeItemCreation(Context context, XmlWorkflowItem workflowItem, Item institutionItem,
        ConcytecFeedback concytecFeedback) throws SQLException, AuthorizeException, WorkflowException {

        if (institutionItem != null) {

            Mode originalMode = context.getCurrentMode();
            try {
                context.setMode(Mode.BATCH_EDIT);
                replaceWillBeReferencedWithItemId(context, workflowItem.getItem(), institutionItem);
            } catch (SearchServiceException e) {
                throw new WorkflowException(e);
            } finally {
                context.setMode(originalMode);
            }

        }

        if (concytecFeedback == ConcytecFeedback.REJECT) {
            Item item = installItemService.installItem(context, workflowItem);
            itemService.withdraw(context, item);
            return getCancelActionResult();
        }

        return getCompleteActionResult();
    }

    private void replaceWillBeReferencedWithItemId(Context context, Item item, Item institutionItem)
        throws SQLException, AuthorizeException, SearchServiceException {

        String authority = AuthorityValueService.REFERENCE + "SHADOW::" + institutionItem.getID();
        Iterator<Item> itemIterator = findItemWithWillBeReferencedShadowAuthority(context, authority, item);

        while (itemIterator.hasNext()) {
            Item itemToUpdate = itemIterator.next();
            for (MetadataValue metadataValue : itemToUpdate.getMetadata()) {
                if (authority.equals(metadataValue.getAuthority())) {
                    metadataValue.setAuthority(item.getID().toString());
                }
            }

            itemService.update(context, itemToUpdate);
        }

    }

    private Iterator<Item> findItemWithWillBeReferencedShadowAuthority(Context context, String authority, Item item)
        throws SearchServiceException {

        String relationshipType = itemService.getMetadataFirstValue(item, "relationship", "type", null, Item.ANY);
        if (relationshipType == null) {
            throw new IllegalArgumentException("The given item has no relationship.type: " + item.getID());
        }

        String query = ""; // TODO search by metadata authority

        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setDSpaceObjectFilter(IndexableItem.TYPE);
        discoverQuery.addFilterQueries(query);

        return new DiscoverResultIterator<Item, UUID>(context, discoverQuery);
    }

    private ActionResult getCompleteActionResult() {
        return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
    }

    private ActionResult getCancelActionResult() {
        return new ActionResult(ActionResult.TYPE.TYPE_CANCEL);
    }

    @Override
    public List<String> getOptions() {
        return new ArrayList<String>();
    }

}
