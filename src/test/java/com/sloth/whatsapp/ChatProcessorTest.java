package com.sloth.whatsapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sloth.whatsapp.models.ChatAnalysisResult;
import com.sloth.whatsapp.models.ChatMessage;
import com.sloth.whatsapp.utils.ChatProcessor;

class ChatProcessorTest {

    private ChatProcessor chatProcessor;

    @BeforeEach
    void setUp() {
        chatProcessor = new ChatProcessor();
    }

    @Test
    void testCountWords() {
        assertEquals(5, chatProcessor.countWords("Isso é um teste simples"));
        assertEquals(0, chatProcessor.countWords(""));
        assertEquals(0, chatProcessor.countWords("    "));
    }

    @Test
    void testParseDate() {
        LocalDateTime dateTime = chatProcessor.parseDate("15/09/2023 18:45");
        assertEquals(15, dateTime.getDayOfMonth());
        assertEquals(9, dateTime.getMonthValue());
        assertEquals(2023, dateTime.getYear());
        assertEquals(18, dateTime.getHour());
        assertEquals(45, dateTime.getMinute());
    }

    @Test
    void testFormatTimeDifference() {
        assertEquals("1 dias, 2 horas, 30 minutos, 45 segundos", chatProcessor.formatTimeDifference(93645000));
        assertEquals("0 dias, 0 horas, 0 minutos, 0 segundos", chatProcessor.formatTimeDifference(0));
    }
    
    @Test
    void testProcess() throws Exception {
        String chatContent = 
            "10/03/2024 12:30 - Alice: Olá, tudo bem?\n" +
            "10/03/2024 12:35 - Bob: Sim, e você?\n" +
            "11/03/2024 14:00 - Alice: Estou bem, obrigado!\n" +
            "11/03/2024 14:01 - Bob: Estou bem, obrigado!\n";
        
        InputStream inputStream = new ByteArrayInputStream(chatContent.getBytes());
        var result = chatProcessor.process(inputStream);
        
        assertEquals(4, result.totalMessages());
        assertEquals("10/03/2024 12:30", result.messages().get(0).date());
        assertEquals("Alice", result.messages().get(0).sender());
        assertEquals("Olá, tudo bem?", result.messages().get(0).content());
    }
}
