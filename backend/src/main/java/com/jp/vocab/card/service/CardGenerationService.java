package com.jp.vocab.card.service;

import com.jp.vocab.card.entity.CardInstanceEntity;
import com.jp.vocab.card.repository.CardInstanceRepository;
import com.jp.vocab.studyplan.entity.StudyPlanEntity;
import com.jp.vocab.wordset.entity.WordEntryEntity;
import com.jp.vocab.wordset.repository.WordEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class CardGenerationService {

    private final WordEntryRepository wordEntryRepository;
    private final CardInstanceRepository cardInstanceRepository;

    public CardGenerationService(
            WordEntryRepository wordEntryRepository,
            CardInstanceRepository cardInstanceRepository
    ) {
        this.wordEntryRepository = wordEntryRepository;
        this.cardInstanceRepository = cardInstanceRepository;
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

            for (int stageNo = 0; stageNo < studyPlan.getReviewOffsets().size(); stageNo++) {
                Integer offset = studyPlan.getReviewOffsets().get(stageNo);
                instances.add(CardInstanceEntity.create(
                        studyPlan.getId(),
                        wordEntry.getId(),
                        offset == 0 ? "NEW" : "REVIEW",
                        sequenceNo,
                        stageNo,
                        sequenceBaseDate.plusDays(offset),
                        "PENDING"
                ));
            }
        }

        cardInstanceRepository.saveAll(instances);
    }
}
