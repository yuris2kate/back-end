package com.sloth.whatsapp.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sloth.whatsapp.models.ChatAnalysisResult;
import com.sloth.whatsapp.models.Conquista;
import com.sloth.whatsapp.models.ConquistaResponse;
import com.sloth.whatsapp.utils.ChatProcessor;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*") // Permite CORS para o frontend
public class ChatController {

    private final ChatProcessor chatProcessor;

    public ChatController(ChatProcessor chatProcessor) {
        this.chatProcessor = chatProcessor;
    }

    @PostMapping(value = "/process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> processWhatsAppChat(
            @RequestParam("file") MultipartFile file) {
        
        // Validação básica
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Arquivo vazio");
        }

        // Verificação explícita do tamanho (redundante, mas segura)
        if (file.getSize() > 80 * 1024 * 1024) {
            return ResponseEntity.badRequest()
                .body("Tamanho do arquivo excede 80MB");
        }

        try {
            ChatAnalysisResult result = chatProcessor.process(file.getInputStream());
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                .body("Erro no processamento: " + e.getMessage());
        }
    }
    
    @GetMapping("/conquistas")
    public ConquistaResponse getConquistas() {
        Map<String, Conquista> conquistas = new HashMap<>();

        // Exemplo de como você pode mapear as conquistas
        conquistas.put("maisEuTeAmo", new Conquista("João", 12));
        conquistas.put("maisMensagens", new Conquista("João", 1200));
        conquistas.put("emojiFavorito", new Conquista("Maria", 45));
        conquistas.put("maiorSequencia", new Conquista("Maria", 15));
        conquistas.put("maiorVacuo", new Conquista("João", 72));
        conquistas.put("maisElogios", new Conquista("João", 23));

        // Você pode adicionar as outras conquistas da mesma forma
        // Conquista para cada uma das métricas que você possui

        return new ConquistaResponse(conquistas);
    }
    
    @PostMapping("/test-upload")
    public ResponseEntity<String> testUpload(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok("Arquivo recebido! Tamanho: " + file.getSize() + " bytes");
    }
}