package com.voicerag.service;

import com.voicerag.config.VoiceRagConfig;
import com.voicerag.model.TranscriptionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Service
public class TranscriptionService {
    private static final Logger log = LoggerFactory.getLogger(TranscriptionService.class);
    private static final Set<String> AUDIO_EXTENSIONS = Set.of(".mp3", ".m4a", ".wav", ".webm", ".mp4", ".ogg", ".flac");

    private final VoiceRagConfig config;

    public TranscriptionService(VoiceRagConfig config) {
        this.config = config;
    }

    public List<TranscriptionStatus> transcribeAll() {
        List<TranscriptionStatus> results = new ArrayList<>();
        Path recordingsPath = Path.of(config.getRecordingsDir());
        Path transcriptsPath = Path.of(config.getTranscriptsDir());

        try {
            Files.createDirectories(transcriptsPath);
        } catch (IOException e) {
            results.add(new TranscriptionStatus("", "error", "Failed to create transcripts directory: " + e.getMessage()));
            return results;
        }

        if (!Files.exists(recordingsPath)) {
            results.add(new TranscriptionStatus("", "error", "Recordings directory does not exist: " + recordingsPath));
            return results;
        }

        try (Stream<Path> files = Files.list(recordingsPath)) {
            List<Path> audioFiles = files
                    .filter(p -> AUDIO_EXTENSIONS.contains(getExtension(p).toLowerCase()))
                    .toList();

            if (audioFiles.isEmpty()) {
                results.add(new TranscriptionStatus("", "info", "No audio files found in " + recordingsPath));
                return results;
            }

            for (Path audioFile : audioFiles) {
                TranscriptionStatus status = transcribeFile(audioFile, transcriptsPath);
                results.add(status);
            }
        } catch (IOException e) {
            results.add(new TranscriptionStatus("", "error", "Failed to list recordings: " + e.getMessage()));
        }

        return results;
    }

    private TranscriptionStatus transcribeFile(Path audioFile, Path transcriptsPath) {
        String baseName = getBaseName(audioFile);
        Path outputFile = transcriptsPath.resolve(baseName + ".txt");

        if (Files.exists(outputFile)) {
            return new TranscriptionStatus(audioFile.getFileName().toString(), "skipped", "Already transcribed");
        }

        log.info("Transcribing: {}", audioFile.getFileName());

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "whisper",
                    audioFile.toString(),
                    "--model", config.getWhisperModel(),
                    "--output_format", "txt",
                    "--output_dir", transcriptsPath.toString()
            );
            pb.redirectErrorStream(true);

            Process process = pb.start();
            StringBuilder output = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    log.debug(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return new TranscriptionStatus(audioFile.getFileName().toString(), "success", "Transcribed successfully");
            } else {
                return new TranscriptionStatus(audioFile.getFileName().toString(), "error", "Whisper failed: " + output);
            }
        } catch (IOException e) {
            return new TranscriptionStatus(audioFile.getFileName().toString(), "error",
                    "Failed to run whisper. Make sure whisper is installed (pip install openai-whisper): " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new TranscriptionStatus(audioFile.getFileName().toString(), "error", "Transcription interrupted");
        }
    }

    private String getExtension(Path path) {
        String name = path.getFileName().toString();
        int lastDot = name.lastIndexOf('.');
        return lastDot > 0 ? name.substring(lastDot) : "";
    }

    private String getBaseName(Path path) {
        String name = path.getFileName().toString();
        int lastDot = name.lastIndexOf('.');
        return lastDot > 0 ? name.substring(0, lastDot) : name;
    }
}
