package com.resumematcher.resumefit.service;


import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class ResumeParsingService {

    public String extractText(MultipartFile file) throws Exception{
        log.info("Extracting text from uploaded file: {}",file.getOriginalFilename());

        try(PDDocument document = Loader.loadPDF(file.getBytes())){
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            log.info("Extracted {} characters from PDF",text.length());

            return text;
        }
    }
}
