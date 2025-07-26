package it.aredegalli.printer.controller.api.slicing;

import it.aredegalli.printer.service.log.LogService;
import it.aredegalli.printer.service.slicing.engine.SlicingEngine;
import it.aredegalli.printer.service.slicing.engine.SlicingEngineSelector;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/slicing/engine")
@RequiredArgsConstructor
public class SlicingEngineController {

    private final LogService log;

    private final SlicingEngineSelector engineSelector;

    @GetMapping()
    public ResponseEntity<Map<String, Object>> getAvailableEngines() {
        log.info("SlicingEngineController", "Getting available slicing engines");

        Map<String, Object> response = new HashMap<>();

        try {
            Map<String, String> engines = engineSelector.getAvailableEngines();
            SlicingEngine defaultEngine = engineSelector.getDefaultEngine();

            response.put("available_engines", engines);
            response.put("default_engine", defaultEngine.getName());
            response.put("default_version", defaultEngine.getVersion());
            response.put("total_count", engines.size());
            response.put("status", "success");

        } catch (Exception e) {
            log.error("SlicingEngineController", "Failed to get engines: " + e.getMessage());
            response.put("status", "error");
            response.put("error", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{engineName}")
    public ResponseEntity<Map<String, Object>> getEngineInfo(@PathVariable String engineName) {
        log.info("SlicingEngineController", "Getting engine info for: " + engineName);

        Map<String, Object> response = new HashMap<>();

        try {
            boolean available = engineSelector.isEngineAvailable(engineName);

            if (available) {
                SlicingEngine engine = engineSelector.getEngine(engineName);
                response.put("name", engine.getName());
                response.put("version", engine.getVersion());
                response.put("available", true);
                response.put("status", "success");
            } else {
                response.put("name", engineName);
                response.put("available", false);
                response.put("status", "not_found");
                response.put("message", "Engine not available");
            }

        } catch (Exception e) {
            log.error("SlicingEngineController", "Failed to get engine info: " + e.getMessage());
            response.put("status", "error");
            response.put("error", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }
}