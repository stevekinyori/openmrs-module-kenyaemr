/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */

package org.openmrs.module.kenyaemr.page.controller;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.openmrs.api.context.Context;
import org.openmrs.module.appframework.AppUiUtil;
import org.openmrs.module.kenyaemr.KenyaEmr;
import org.openmrs.module.kenyaemr.reporting.builder.ReportBuilder;
import org.openmrs.module.reporting.common.ContentType;
import org.openmrs.module.reporting.common.DateUtil;
import org.openmrs.module.reporting.evaluation.EvaluationContext;
import org.openmrs.module.reporting.report.ReportData;
import org.openmrs.module.reporting.report.ReportDesign;
import org.openmrs.module.reporting.report.ReportDesignResource;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.reporting.report.definition.service.ReportDefinitionService;
import org.openmrs.module.reporting.report.renderer.ExcelTemplateRenderer;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.FileDownload;
import org.openmrs.ui.framework.page.PageModel;
import org.openmrs.ui.framework.session.Session;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Rum monthly indicator report page controller
 */
public class ReportsRunMonthlyIndicatorReportPageController {
	
	public Object controller(Session session,
					PageModel model,
					@SpringBean KenyaEmr emr,
					@RequestParam("builder") String builderClassname,
					@RequestParam(required = false, value = "mode") String mode,
					@RequestParam(required = false, value = "startDate") Date startDate) throws Exception {
		
		AppUiUtil.startApp("kenyaemr.reports", session);
		
		ReportBuilder builder = emr.getReportManager().getReportBuilder(builderClassname);
		ReportDefinition definition = builder.getReportDefinition();

		model.addAttribute("builder", builder);
		model.addAttribute("definition", definition);
		
		if (startDate != null) {
			// generate the report
			EvaluationContext ec = new EvaluationContext();
			ec.addParameterValue("startDate", startDate);
			ec.addParameterValue("endDate", DateUtil.getEndOfMonth(startDate));
			ReportData data = Context.getService(ReportDefinitionService.class).evaluate(definition, ec);
			
			if ("excel".equals(mode)) {
				if (!builder.isExcelRenderable()) {
					throw new RuntimeException(builderClassname + " does not support Excel output");
				}

				byte[] excelTemplate = builder.loadExcelTemplate();
				
				ExcelTemplateRenderer renderer;
				{
					// this is a bit of a hack, copied from ExcelRendererTest in the reporting module, to avoid
					// needing to save the template and report design in the database
					ReportDesignResource resource = new ReportDesignResource();
					resource.setName("template.xls");
					resource.setContents(excelTemplate);
					
					final ReportDesign design = new ReportDesign();
					design.setName(definition.getName() + " design");
					design.setReportDefinition(definition);
					design.setRendererType(ExcelTemplateRenderer.class);
					design.addResource(resource);
					
					renderer = new ExcelTemplateRenderer() {
						public ReportDesign getDesign(String argument) {
							return design;
						}
					};
				}
				
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				renderer.render(data, "xxx:xls", out);
				return new FileDownload(builder.getExcelDownloadFilename(ec), ContentType.EXCEL.getContentType(), out.toByteArray());
				
			} else {
				model.addAttribute("data", data);
			}
			
		} else {
			// show form with date options
			model.addAttribute("data", null);
			Map<String, String> startDateOptions = new LinkedHashMap<String, String>();
			SimpleDateFormat ymd = new SimpleDateFormat("yyyy-MM-dd");
			SimpleDateFormat pretty = new SimpleDateFormat("MMMM yyyy");
			Date d = DateUtil.getStartOfMonth(new Date());
			for (int i = 0; i < 6; ++i) {
				d = DateUtil.getStartOfMonth(d, -1);
				startDateOptions.put(ymd.format(d), pretty.format(d));
			}
			model.addAttribute("startDateOptions", startDateOptions);
		}
		return null;
	}
	
}
