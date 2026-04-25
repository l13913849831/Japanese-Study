package com.jp.vocab.card.service;

import com.jp.vocab.card.entity.CardInstanceEntity;
import com.jp.vocab.card.repository.CardInstanceRepository;
import com.jp.vocab.studyplan.entity.StudyPlanEntity;
import com.jp.vocab.wordset.entity.WordEntryEntity;
import com.jp.vocab.wordset.repository.WordEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
public class CardGenerationService {

    private final WordEntryRepository wordEntryRepository;
    private final CardInstanceRepository cardInstanceRepository;
    private final CardFsrsScheduler cardFsrsScheduler;

    public CardGenerationService(
            WordEntryRepository wordEntryRepository,
            CardInstanceRepository cardInstanceRepository,
            CardFsrsScheduler cardFsrsScheduler
    ) {
        this.wordEntryRepository = wordEntryRepository;
        this.cardInstanceRepository = cardInstanceRepository;
        this.cardFsrsScheduler = cardFsrsScheduler;
    }

    @Transactional
    public void regenerateForPlan(StudyPlanEntity studyPlan) {
        cardInstanceRepository.deleteByPlanId(studyPlan.getId());

        List<WordEntryEntity> wordEntries = wordEntryRepository.findByWordSetIdOrderBySourceOrderAsc(studyPlan.getWordSetId());
        List<CardInstanceEntity> instances = new ArrayList<>();

        for (int index = 0; index < wordEntries.size(); index++) {
            WordEntryEntity wordEntry = wordEntries.get(index);
            int sequenceNo = (index / studyPlan.getDailyNewCount()) + 1;
            LocalDate sequenceBaseDate = studyPlan.getStartDate().plusDays(sequenceNo - 1L);
            OffsetDateTime dueAt = sequenceBaseDate.atStartOfDay().atOffset(ZoneOffset.UTC);
            CardFsrsScheduler.InitialCardState initialState = cardFsrsScheduler.createInitialState();
            instances.add(CardInstanceEntity.createFsrsCard(
                    studyPlan.getId(),
                    wordEntry.getId(),
                    "NEW",
                    sequenceNo,
                    0,
                    dueAt,
                    "PENDING",
                    initialState.fsrsCardJson(),
                    0,
                    null
            ));
        }

        cardInstanceRepository.saveAll(instances);
    }
}
