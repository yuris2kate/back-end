package com.sloth.whatsapp.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sloth.whatsapp.models.ChatAnalysisResult;
import com.sloth.whatsapp.models.ChatMessage;
import com.sloth.whatsapp.models.Conquista;

@Service
public class ChatProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(ChatProcessor.class);
	private static final Pattern MESSAGE_PATTERN = Pattern
			.compile("^(\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}) - ([^:]+): (.+)$");

	public ChatAnalysisResult process(InputStream inputStream) throws IOException {

		LOGGER.info("Iniciando processamento do arquivo de chat");

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

			List<ChatMessage> messages = new ArrayList<>();
			Map<String, Long> maiorVacuo = new LinkedHashMap<>();
			long maxTimeDifference = 0;
			long lastTimestamp = 0;
			String lastSender = null;

			String line;
			int contador = 0;
			while ((line = reader.readLine()) != null) {
				
				Matcher matcher = MESSAGE_PATTERN.matcher(line);

				if (matcher.find()) {
					String dateStr = matcher.group(1);
					String sender = matcher.group(2);
					String content = matcher.group(3);

					LocalDateTime dateTime = parseDate(dateStr);

					// Vacuo e contagem de tempo
					long currentTimestamp = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

					if (lastSender != null) {
						maiorVacuo.merge(sender, currentTimestamp - lastTimestamp, Long::max);
					}
					if (lastTimestamp != 0 && lastSender != sender) {
						maxTimeDifference = Math.max(maxTimeDifference, currentTimestamp - lastTimestamp);
					}
					lastSender = sender;
					lastTimestamp = currentTimestamp;

					messages.add(new ChatMessage(dateStr, sender, content));
				}
			}

			// Chama cálculo de resumo semanal
			Map<String, Map<String, Integer>> resumo = calcularResumoSemanal(messages);
			Map<String, Integer> charCountByDay = resumo.get("charCountByDay");
			Map<String, Integer> wordCountByDay = resumo.get("wordCountByDay");
			Map<String, Conquista> conquistas = new LinkedHashMap<>();
			List<Map<String, Object>> nuvemPalavras = gerarNuvemDePalavras(messages);

			conquistas.put("maisEuTeAmo", calcularMaisEuTeAmo(messages));
			conquistas.put("maisEuTambemTeAmo", CalcularMaisEuTambemTeAmo(messages));
			conquistas.put("mensagemComMaisCarectere", mensagemComMaisCaracteres(messages));
			conquistas.put("maisMidiaOculta", maisMidiaOculta(messages));
			conquistas.put("maisMandouMensagem", quemMaisMandouMensagem(messages));
			conquistas.put("maiorSequenciaMensagens", maiorSequenciaMensagens(messages));
			conquistas.put("maisMensagensMadrugada", maisMensagensMadrugada(messages));
			conquistas.put("maisBomdia", quemDeuMaisBomDiaPrimeiro(messages));
			
			return new ChatAnalysisResult(
					List.copyOf(charCountByDay.keySet()),
					new ArrayList<>(charCountByDay.values()),
					new ArrayList<>(wordCountByDay.values()),
					nuvemPalavras,
					formatTimeDifference(maxTimeDifference),
					messages.size(),
					conquistas,
					List.copyOf(messages));
		}

	}
	
	public List<Map<String, Object>> gerarNuvemDePalavras(List<ChatMessage> messages) {
		Set<String> palavrasIgnoradas = Set.of(
		        "de", "a", "o", "e", "que", "do", "da", "em", "um", "uma", "pra", "é", "com", "não", "para", "na", "no", "se", "vc", "por", "mas"
		    );

		    Map<String, Integer> contagem = new HashMap<>();

		    for (ChatMessage msg : messages) {
		        String[] palavras = msg.content().toLowerCase().split("\\W+");
		        for (String palavra : palavras) {
		            if (palavra.isBlank() || palavrasIgnoradas.contains(palavra) || palavra.length() == 1) continue;
		            contagem.put(palavra, contagem.getOrDefault(palavra, 0) + 1);
		        }
		    }

		 // Ordena por frequência
		    return contagem.entrySet().stream()
		            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
		            .limit(20)
		            .map(entry -> {
		                Map<String, Object> palavraJson = new HashMap<>();
		                palavraJson.put("text", entry.getKey());
		                palavraJson.put("size", entry.getValue());
		                return palavraJson;
		            })
		            .toList();
	}

	public Conquista maisMidiaOculta(List<ChatMessage> messages) {
		Map<String, Integer> contador = new HashMap<>();
	
		for (ChatMessage msg : messages) {
			if ("<mídia oculta>".equalsIgnoreCase(msg.content().trim())) {
				contador.merge(msg.sender(), 1, Integer::sum);
			}
		}
	
		return getMaiorContador(contador);
	}
	
	public Conquista maisMensagensMadrugada(List<ChatMessage> messages) {
	    Map<String, Integer> contador = new HashMap<>();

	    for (ChatMessage msg : messages) {
	        LocalDateTime timestamp = parseDate(msg.date());
	        int hora = timestamp.getHour();

	        // Verifica se a hora está entre 2h (inclusive) e 6h (exclusive)
	        if (hora >= 2 && hora < 7) {
	            contador.merge(msg.sender(), 1, Integer::sum);
	        }
	    }

	    return getMaiorContador(contador);
	}
	
	public Conquista quemDeuMaisBomDiaPrimeiro(List<ChatMessage> messages) {
	    Map<String, Integer> contador = new HashMap<>();
	    String ultimoDiaProcessado = "";

	    for (ChatMessage msg : messages) {
	        LocalDateTime dateTime = parseDate(msg.date());
	        String dia = dateTime.toLocalDate().toString(); // Ex: "2023-04-20"

	        // Se já processamos esse dia, ignoramos
	        if (dia.equals(ultimoDiaProcessado)) {
	            continue;
	        }

	        // Verifica se contém bom dia (ignora maiúsculas, acentos e variações simples)
	        String content = msg.content().toLowerCase().replaceAll("[^a-zA-Z]", "");
	        if (content.contains("bomdia")) {
	            contador.merge(msg.sender(), 1, Integer::sum);
	            ultimoDiaProcessado = dia; // Marcamos esse dia como já processado
	        }
	    }

	    return getMaiorContador(contador);
	}

	
	public Conquista maiorSequenciaMensagens(List<ChatMessage> messages) {
	    String atual = null;
	    int sequenciaAtual = 0;

	    String campeao = null;
	    int maiorSequencia = 0;

	    for (ChatMessage msg : messages) {
	        String remetente = msg.sender();

	        if (remetente.equals(atual)) {
	            sequenciaAtual++;
	        } else {
	            atual = remetente;
	            sequenciaAtual = 1;
	        }

	        if (sequenciaAtual > maiorSequencia) {
	            maiorSequencia = sequenciaAtual;
	            campeao = remetente;
	        }
	    }

	    return new Conquista(campeao, maiorSequencia);
	}
	
	public Conquista quemMaisMandouMensagem(List<ChatMessage> messages) {
	    Map<String, Integer> contador = new HashMap<>();

	    for (ChatMessage msg : messages) {
	        contador.merge(msg.sender(), 1, Integer::sum);
	    }

	    return getMaiorContador(contador);
	}

	private Conquista calcularMaisEuTeAmo(List<ChatMessage> mensagens) {
		Map<String, Integer> contadorPorPessoa = new HashMap<>();
		int contador = 0;
	
		// Regex flexível para pegar variações de "te amo", "amo te" etc.
		Pattern regexTeAmo = Pattern.compile("(?i)(eu\\s*)?(te\\s*amo|amo\\s*te)");
	
		List<String> palavrasProibidas = List.of("também", "tambem", "tmb");
	
		for (ChatMessage mensagem : mensagens) {
			String conteudo = mensagem.content().toLowerCase();
	
			// Ignora mensagens com palavras proibidas
			boolean contemProibida = palavrasProibidas.stream().anyMatch(conteudo::contains);
			if (contemProibida) continue;
	
			Matcher matcher = regexTeAmo.matcher(conteudo);
			if (matcher.find()) {
				contadorPorPessoa.merge(mensagem.sender(), 1, Integer::sum);
			}
		}
	
		// Acha o maior
		return contadorPorPessoa.entrySet().stream()
				.max(Map.Entry.comparingByValue())
				.map(entry -> new Conquista(entry.getKey(), entry.getValue()))
				.orElse(new Conquista("Ninguém", 0));
	}

	public Conquista CalcularMaisEuTambemTeAmo(List<ChatMessage> messages) {
		Map<String, Integer> contador = new HashMap<>();
	
		// Lista de variações de "também"
		List<String> palavrasTambem = List.of("também", "tambem", "tmb", "tbm");
	
		for (ChatMessage msg : messages) {
			String content = msg.content().toLowerCase();
	
			// Verifica se contém "eu te amo" (ou amo te) e também alguma das palavras de variação de "também"
			if ((content.contains("eu te amo") || content.contains("amo te") || content.contains("te amo")) &&
				palavrasTambem.stream().anyMatch(content::contains)) {
				
				contador.merge(msg.sender(), 1, Integer::sum);
			}
		}
	
		return getMaiorContador(contador);
	}
	
	public Conquista mensagemComMaisCaracteres(List<ChatMessage> messages) {
	    String autor = null;
	    String maiorMensagem = "";
	    int maxCaracteres = 0;

	    for (ChatMessage msg : messages) {
	        String content = msg.content();
	        if (content.length() > maxCaracteres) {
	            maxCaracteres = content.length();
	            autor = msg.sender();
	            maiorMensagem = content;
	        }
	    }

	    LOGGER.info("Mensagem com mais caracteres:");
	    LOGGER.info("Autor: {}", autor);
	    LOGGER.info("Caracteres: {}", maxCaracteres);
	    LOGGER.info("Conteúdo: {}", maiorMensagem);

	    return new Conquista(autor, maxCaracteres);
	}
	

	public Map<String, Map<String, Integer>> calcularResumoSemanal(List<ChatMessage> messages) {
		Map<String, Integer> charCountByDay = new LinkedHashMap<>();
		Map<String, Integer> wordCountByDay = new LinkedHashMap<>();

		String[] daysOfWeek = { "Segunda-feira", "Terça-feira", "Quarta-feira", "Quinta-feira", "Sexta-feira",
				"Sábado", "Domingo" };
		for (String day : daysOfWeek) {
			charCountByDay.put(day, 0);
			wordCountByDay.put(day, 0);
		}

		for (ChatMessage message : messages) {
			LocalDateTime dateTime = parseDate(message.date());
			String dayOfWeek = dateTime.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("pt", "BR"));
			dayOfWeek = Character.toUpperCase(dayOfWeek.charAt(0)) + dayOfWeek.substring(1);

			charCountByDay.merge(dayOfWeek, message.content().length(), Integer::sum);
			wordCountByDay.merge(dayOfWeek, countWords(message.content()), Integer::sum);
		}

		Map<String, Map<String, Integer>> resultado = new LinkedHashMap<>();
		resultado.put("charCountByDay", charCountByDay);
		resultado.put("wordCountByDay", wordCountByDay);
		return resultado;
	}
	
	private Conquista getMaiorContador(Map<String, Integer> contador) {
		String ganhador = null;
		int maior = 0;

		for (Map.Entry<String, Integer> entry : contador.entrySet()) {
			if (entry.getValue() > maior) {
				ganhador = entry.getKey();
				maior = entry.getValue();
			}
		}

		if (ganhador == null) {
			return new Conquista("Ninguém 😢", 0);
		}

		return new Conquista(ganhador, maior);
	}


	public int countWords(String message) {
		return message == null || message.isBlank() ? 0 : message.split("\\s+").length;
	}

	public LocalDateTime parseDate(String dateStr) {
		return LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
	}

	public String formatTimeDifference(long milliseconds) {
		long seconds = milliseconds / 1000;
		long minutes = seconds / 60;
		long hours = minutes / 60;
		long days = hours / 24;

		return String.format("%d dias, %d horas, %d minutos, %d segundos", days, hours % 24, minutes % 60,
				seconds % 60);
	}
}
