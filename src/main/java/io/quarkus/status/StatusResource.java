package io.quarkus.status;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.status.model.Stats;
import io.quarkus.status.model.StatsEntry;
import io.quarkus.status.model.StatsPerArea;
import io.quarkus.status.model.Status;

import java.util.List;
import java.util.Map;

@Path("/")
public class StatusResource {

    @Inject
    StatusService statusService;

    @Inject
    IssuesService issuesService;

    @Inject
    LabelsService labelsService;

    @CheckedTemplate(requireTypeSafeExpressions = false)
    public static class Templates {
        public static native TemplateInstance index(Status status);
        public static native TemplateInstance issues(Status status, Stats stats, boolean isBugs);
        public static native TemplateInstance issuesPerArea(Status status, StatsPerArea statsPerArea, boolean isBugs);
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance index() throws Exception {
        return Templates.index(statusService.getStatus());
    }

    @GET
    @Path("bugs")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance bugs() throws Exception {
        return Templates.issues(statusService.getStatus(), issuesService.getBugsMonthlyStats(), true);
    }

    @GET
    @Path("enhancements")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance features() throws Exception {
        return Templates.issues(statusService.getStatus(), issuesService.getEnhancementsMonthlyStats(), false);
    }

    @GET
    @Path("bugs/per-area")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance bugsPerArea() throws Exception {
        StatsPerArea statsPerArea = issuesService.getIssuesPerArea();
        StringBuilder sb = new StringBuilder();
        for(Map.Entry<String, StatsPerArea.DatasetDetails> areaEntries : statsPerArea.perAreaMap.entrySet()) {
            sb.append(areaEntries.getKey());
            sb.append(": ");
            areaEntries.getValue().statsEntries.forEach(statsEntry -> sb.append(statsEntry.entryName + ":" + statsEntry.created + ", "));
            sb.append("\n");
        }
        System.out.println(sb.toString());
        return Templates.issuesPerArea(statusService.getStatus(), statsPerArea, true);
    }

    @GET
    @Path("labels/bugs")
    @Produces(MediaType.TEXT_PLAIN)
    public String bugsLabels() throws Exception {
        StringBuilder sb = new StringBuilder();
        labelsService.getBugsLabels().forEach( label -> sb.append(label).append("\n"));
        return sb.toString();
    }

    @GET
    @Path("labels/enhancements")
    @Produces(MediaType.TEXT_PLAIN)
    public String enhancementsLabels() throws Exception {
        StringBuilder sb = new StringBuilder();
        labelsService.getEnhancementsLabels().forEach( label -> sb.append(label).append("\n"));
        return sb.toString();
    }
}
