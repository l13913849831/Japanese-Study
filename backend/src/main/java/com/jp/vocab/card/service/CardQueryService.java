package com.jp.vocab.card.service;

import com.jp.vocab.card.dto.CardCalendarItemResponse;
import com.jp.vocab.card.dto.TodayCardResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class CardQueryService {

    @Transactional(readOnly = true)
    public List<TodayCardResponse> getTodayCards(Long planId, LocalDate date) {
        return List.of();
    }

    @Transactional(readOnly = true)
    public List<CardCalendarItemResponse> getCalendar(Long planId, LocalDate start, LocalDate end) {
        return List.of();
    }
}
