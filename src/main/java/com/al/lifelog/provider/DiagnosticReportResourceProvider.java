package com.al.lifelog.provider;

import com.al.lifelog.service.DiagnosticReportService;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.server.SimpleBundleProvider;
import java.util.List;

@Component
public class DiagnosticReportResourceProvider implements IResourceProvider {

    private final DiagnosticReportService diagnosticReportService;

    @Autowired
    public DiagnosticReportResourceProvider(DiagnosticReportService diagnosticReportService) {
        this.diagnosticReportService = diagnosticReportService;
    }

    @Override
    public Class<DiagnosticReport> getResourceType() {
        return DiagnosticReport.class;
    }

    @Read
    public DiagnosticReport read(@IdParam IdType theId) {
        DiagnosticReport report = diagnosticReportService.getDiagnosticReport(theId.getIdPart());
        if (report == null) {
            throw new ResourceNotFoundException(theId);
        }
        return report;
    }

    @Create
    public ca.uhn.fhir.rest.api.MethodOutcome create(@ResourceParam DiagnosticReport theReport) {
        DiagnosticReport created = diagnosticReportService.createDiagnosticReport(theReport);
        return new ca.uhn.fhir.rest.api.MethodOutcome(new IdType(created.getId()));
    }

    @Search
    public IBundleProvider search(
            @OptionalParam(name = DiagnosticReport.SP_SUBJECT) StringParam subject,
            @OptionalParam(name = DiagnosticReport.SP_CODE) StringParam code,
            @OptionalParam(name = "_count") ca.uhn.fhir.rest.param.NumberParam count,
            @OptionalParam(name = "_offset") ca.uhn.fhir.rest.param.NumberParam offset) {

        String subjectVal = (subject != null) ? subject.getValue() : null;
        String codeVal = (code != null) ? code.getValue() : null;

        int countVal = (count != null) ? count.getValue().intValue() : 10;
        int offsetVal = (offset != null) ? offset.getValue().intValue() : 0;

        List<DiagnosticReport> reports = diagnosticReportService.searchDiagnosticReports(subjectVal, codeVal, offsetVal,
                countVal);
        return new SimpleBundleProvider(reports);
    }
}
