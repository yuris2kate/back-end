package com.sloth.whatsapp.models;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record ChatAnalysisResult(
	    List<String> daysOfWeek,
	    List<Integer> charCountByDay,
	    List<Integer> wordCountByDay,
	    List<Map<String, Object>> nuvemDePalavras,
	    String maxTimeDifference,
	    int totalMessages,
	    Map<String, Conquista> conquista,
		@JsonIgnore
	    List<ChatMessage> messages
	) {}