package com.resumematcher.resumefit.service;

import com.resumematcher.resumefit.dto.request.InsightRequestDto;
import com.resumematcher.resumefit.dto.request.MissingSkillContextDto;
import com.resumematcher.resumefit.dto.response.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InsightGenerationService {

    private final ChatClient chatClient;

    public InsightsDto generateInsights(ParsedResumeDto resume,
                                        GapAnalysisDto gapAnalysis,
                                        SalaryAnalysisDto salaryAnalysis,
                                        List<JobMatchDto> topMatches) {

        InsightRequestDto context = buildContext(resume, gapAnalysis, salaryAnalysis, topMatches);
        log.info("Generating insights for {} seniority profile with {} missing skills",
                context.getSeniority(), context.getMissingSkills().size());

        String prompt = buildPrompt(context);
        log.debug("Insight prompt length: {} characters", prompt.length());

        InsightsDto insights = chatClient.prompt()
                .user(prompt)
                .call()
                .entity(InsightsDto.class);

        if (insights == null) {
            log.error("Gemini returned null insights — check prompt structure");
            return InsightsDto.builder()
                    .overallAssessment("Unable to generate insights at this time.")
                    .skillInsights(List.of())
                    .resumeSuggestions(List.of())
                    .build();
        }

        log.info("Insights generated. Skills with insights: {}, Resume suggestions: {}",
                insights.getSkillInsights() != null ? insights.getSkillInsights().size() : 0,
                insights.getResumeSuggestions() != null ? insights.getResumeSuggestions().size() : 0);

        return insights;
    }

    private InsightRequestDto buildContext(ParsedResumeDto resume,
                                           GapAnalysisDto gapAnalysis,
                                           SalaryAnalysisDto salaryAnalysis,
                                           List<JobMatchDto> topMatches) {

        List<MissingSkillContextDto> missingSkills = gapAnalysis.getMissingSkills().stream()
                .map(g -> MissingSkillContextDto.builder()
                        .skill(g.getSkill())
                        .appearsInJobs(g.getAppearsInJobs())
                        .outOf(g.getOutOf())
                        .build())
                .collect(Collectors.toList());

        List<String> topJobTitles = topMatches.stream()
                .map(JobMatchDto::getJobTitle)
                .distinct()
                .limit(5)
                .collect(Collectors.toList());

        String currentAvg = salaryAnalysis.getCurrentRange() != null
                && salaryAnalysis.getCurrentRange().getAvg() != null
                ? salaryAnalysis.getCurrentRange().getAvg() + " LPA"
                : "unavailable";

        String postAvg = salaryAnalysis.getUpliftLpa() != null
                && salaryAnalysis.getUpliftLpa().compareTo(java.math.BigDecimal.ZERO) > 0
                ? salaryAnalysis.getPostSkillRange().getAvg() + " LPA"
                : "similar to current (these skills expand your job opportunities more than your salary at this level)";

        String uplift = salaryAnalysis.getUpliftLpa() != null
                ? salaryAnalysis.getUpliftLpa() + " LPA"
                : "0 LPA";

        return InsightRequestDto.builder()
                .seniority(resume.getSeniority())
                .education(resume.getEducation())
                .currentRoles(resume.getCurrentRoles())
                .currentSkills(resume.getSkills())
                .resumeSkillsMatched(gapAnalysis.getResumeSkillsMatched())
                .missingSkills(missingSkills)
                .currentSalaryAvg(currentAvg)
                .postSkillSalaryAvg(postAvg)
                .upliftLpa(uplift)
                .topJobTitles(topJobTitles)
                .totalJobsAnalyzed(gapAnalysis.getTotalJobsAnalyzed())
                .build();
    }

    private String buildPrompt(InsightRequestDto ctx) {
        String missingSkillsFormatted = ctx.getMissingSkills().stream()
                .map(s -> "  - " + s.getSkill() + ": " + s.getAppearsInJobs() + "/" + s.getOutOf())
                .collect(Collectors.joining("\n"));

        String currentSkills = String.join(", ", ctx.getCurrentSkills());
        String roles = String.join(", ", ctx.getCurrentRoles());
        String matchedSkills = String.join(", ", ctx.getResumeSkillsMatched());
        String topJobTitles = String.join(", ", ctx.getTopJobTitles());
        String seniority = ctx.getSeniority();
        int totalJobs = ctx.getTotalJobsAnalyzed();

        return "You are a career advisor analyzing a resume against real job market data.\n"
                + "Be direct, specific, and honest. Do not nitpick minor issues.\n"
                + "Address the candidate directly using 'you' and 'your' — never refer to them in third person.\n\n"
                + "CANDIDATE PROFILE:\n"
                + "- Seniority: " + seniority + "\n"
                + "- Education: " + ctx.getEducation() + "\n"
                + "- Roles: " + roles + "\n"
                + "- Current Skills: " + currentSkills + "\n"
                + "- Skills matching job requirements: " + matchedSkills + "\n\n"
                + "JOB MARKET DATA (" + totalJobs + " jobs analyzed):\n"
                + "- Top matched job titles: " + topJobTitles + "\n"
                + "- Missing skills (skill: appears in X out of " + totalJobs + " jobs):\n"
                + missingSkillsFormatted + "\n\n"
                + "SALARY DATA:\n"
                + "- Current expected average: " + ctx.getCurrentSalaryAvg() + "\n"
                + "- Expected average after learning top missing skills: " + ctx.getPostSkillSalaryAvg() + "\n"
                + "- Potential uplift: " + ctx.getUpliftLpa() + "\n\n"
                + "Respond ONLY with a valid JSON object matching this exact structure.\n"
                + "No markdown, no code fences, no explanation outside the JSON:\n"
                + "{\n"
                + "  \"overallAssessment\": \"2-3 sentence honest assessment of your current market position. Name specific strengths. Be direct. Use 'you' and 'your'.\",\n"
                + "  \"roleFitAssessment\": \"1-2 sentences on which role your profile most strongly fits. If multiple roles, say which to focus on.\",\n"
                + "  \"recommendedLearningOrder\": [\"skill1\", \"skill2\", \"skill3\"],\n"
                + "  \"skillInsights\": [\n"
                + "    {\n"
                + "      \"skill\": \"skill name\",\n"
                + "      \"explanation\": \"What this skill is, why employers want it at " + seniority + " level, and the fastest way to learn it given your existing skills. 2-3 sentences.\",\n"
                + "      \"quickWin\": true or false,\n"
                + "      \"quickWinReason\": \"If quickWin is true: exactly what to add to the resume given existing skills. If false: null.\"\n"
                + "    }\n"
                + "  ],\n"
                + "  \"salaryContext\": \"1-2 sentences contextualizing the salary numbers. Reference actual figures. Do not overpromise.\",\n"
                + "  \"resumeSuggestions\": [\n"
                + "    \"Only include for glaring issues or obvious quick wins. Do NOT nitpick. Maximum 3, minimum 0.\"\n"
                + "  ]\n"
                + "}\n\n"
                + "Rules:\n"
                + "- resumeSuggestions: ONLY for (a) skills clearly implied by existing experience but not listed, or (b) egregiously bad phrasing. No generic advice.\n"
                + "- quickWin: true if ANY apply:\n"
                + "  (a) existing skills include a framework/tool built on top of this skill\n"
                + "  (b) existing work implies this skill was used in practice without being named\n"
                + "  (c) adjacent skills cover 80%+ of what this skill requires\n"
                + "  Examples: LangChain implies OpenAI API. RAG implies Vector DBs. FastAPI implies REST APIs. Fine-tuning implies LLMs. LLMs implies NLP.\n"
                + "- recommendedLearningOrder: most impactful first (frequency in jobs x salary uplift potential).\n"
                + "- Calibrate all language and advice for a " + seniority + " level candidate.\n"
                + "- Use 'you' and 'your' throughout. Never say 'the candidate'.";
    }
}