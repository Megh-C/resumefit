package com.resumematcher.resumefit.service;


import com.resumematcher.resumefit.dto.response.ParsedResumeDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeExtractionService {

    private final ChatClient chatClient;

    public ParsedResumeDto extractStructuredData(String resumeText){
        log.info("Sending resume text to Gemini for structured extraction. Text length: {}",resumeText.length());

        String promptText = """
                You are a resume parser. Extract structured information from the resume text below.
                
                                Rules:
                                - skills: list every technical skill, tool, framework, or language mentioned. Use the exact terms as they appear.
                                - yearsExperience: total years of professional work experience as a whole number. If unclear, estimate conservatively.
                                - currentRoles: a person can reasonably be described by multiple roles (e.g. both "Data Analyst" and "Backend Developer") if their experience supports it. Return all that apply, not just one.
                                - seniority: one of "Fresher", "Junior", "Mid", "Senior", "Lead" based on years of experience and role complexity.
                                - education: the highest degree mentioned, in short form (e.g. "B.Tech", "M.Tech", "MCA").
                
                                Resume text:
                                %s
                """.formatted(resumeText);

        ParsedResumeDto result = chatClient.prompt()
                .user(promptText)
                .call()
                .entity(ParsedResumeDto.class);

        log.info("Extraction complete. Skills found: {}, Roles: {}, Experience: {} years",
                result.getSkills().size(), result.getCurrentRoles(), result.getYearsExperience());

        return result;

    }

}
