package com.project.urlshortner.controller;
import com.project.urlshortner.model.Url;
import com.project.urlshortner.model.UrlDto;
import com.project.urlshortner.model.UrlErrorResponseDto;
import com.project.urlshortner.model.UrlResponseDto;

import com.project.urlshortner.service.UrlService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;


@RestController
public class UrlShortnerController {

    @Autowired
    private UrlService service;


    @PostMapping("/generate")
    public ResponseEntity<?> generateShortLink(@RequestBody UrlDto urlDto) {
        try {
            Url urlToRet = service.generateShortLink(urlDto);

            if (urlToRet != null) {
                UrlResponseDto urlResponseDto = new UrlResponseDto();
                urlResponseDto.setOriginalUrl(urlToRet.getOriginalUrl());
                urlResponseDto.setExpirationDate(urlToRet.getExpirationTime());
                urlResponseDto.setShortLink(urlToRet.getShortLink());
                return new ResponseEntity<>(urlResponseDto, HttpStatus.OK);
            }

            UrlErrorResponseDto urlErrorResponseDto = new UrlErrorResponseDto();
            urlErrorResponseDto.setStatus("404");
            urlErrorResponseDto.setError("Error while processing your request, please try again");
            return new ResponseEntity<>(urlErrorResponseDto, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            UrlErrorResponseDto urlErrorResponseDto = new UrlErrorResponseDto();
            urlErrorResponseDto.setStatus("400");
            urlErrorResponseDto.setError(e.getMessage());
            return new ResponseEntity<>(urlErrorResponseDto, HttpStatus.BAD_REQUEST);
        }

    }

    @GetMapping("/{shortLink}")
    public ResponseEntity<?> redirectToOriginalUrl(@PathVariable String shortLink, HttpServletResponse response) throws IOException {

        if(StringUtils.isEmpty(shortLink)) {
            UrlErrorResponseDto urlErrorResponseDto = new UrlErrorResponseDto();
            urlErrorResponseDto.setStatus("404");
            urlErrorResponseDto.setError("Invalid Url");
            return new ResponseEntity<>(urlErrorResponseDto, HttpStatus.BAD_REQUEST);
        }
        Url urlToRet = service.getEncodedUrl(shortLink);

        if (urlToRet == null) {
            UrlErrorResponseDto urlErrorResponseDto = new UrlErrorResponseDto();
            urlErrorResponseDto.setStatus("404");
            urlErrorResponseDto.setError("Url doesn't exist or might have been expired");
            return new ResponseEntity<>(urlErrorResponseDto, HttpStatus.NOT_FOUND);
        }

        if (urlToRet.getExpirationTime().isBefore(LocalDateTime.now())) {
            service.deleteShortLink(urlToRet);
            UrlErrorResponseDto urlErrorResponseDto = new UrlErrorResponseDto();
            urlErrorResponseDto.setStatus("200");
            urlErrorResponseDto.setError("Url expired");
            return new ResponseEntity<>(urlErrorResponseDto, HttpStatus.GONE);
        }

        response.sendRedirect(urlToRet.getOriginalUrl());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stats/{shortLink}")
    public ResponseEntity<?> getStats(@PathVariable String shortLink) {
        if (StringUtils.isEmpty(shortLink)) {
            UrlErrorResponseDto error = new UrlErrorResponseDto("400", "Invalid Url format");
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }

        Url url = service.getOriginalUrl(shortLink);
        if (url == null) {
            UrlErrorResponseDto error = new UrlErrorResponseDto("404", "Url doesn't exist");
            return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(url.getClickCount(), HttpStatus.OK);

    }

    @DeleteMapping("/{shortLink}")
    public ResponseEntity<?> deleteShortLink(@PathVariable String shortLink) {
        if (StringUtils.isEmpty(shortLink)) {
            UrlErrorResponseDto error = new UrlErrorResponseDto("400", "Invalid Url request");
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
        Url urlToRet = service.getOriginalUrl(shortLink);

        if (urlToRet == null) {
            UrlErrorResponseDto urlErrorResponseDto = new UrlErrorResponseDto();
            urlErrorResponseDto.setStatus("404");
            urlErrorResponseDto.setError("Url doesn't exist or might have been expired");
            return new ResponseEntity<>(urlErrorResponseDto, HttpStatus.NOT_FOUND);
        }

        service.deleteShortLink(urlToRet);
        UrlErrorResponseDto urlErrorResponseDto = new UrlErrorResponseDto();
        urlErrorResponseDto.setStatus("200");
        urlErrorResponseDto.setError("Url Deleted successfully");
        return new ResponseEntity<>(urlErrorResponseDto, HttpStatus.OK);
    }

}
