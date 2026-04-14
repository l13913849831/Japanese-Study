package com.jp.vocab.wordset.service;

import com.jp.vocab.shared.api.PageResponse;
import com.jp.vocab.wordset.dto.CreateWordSetRequest;
import com.jp.vocab.wordset.dto.WordSetResponse;
import com.jp.vocab.wordset.entity.WordSetEntity;
import com.jp.vocab.wordset.repository.WordSetRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WordSetService {

    private final WordSetRepository wordSetRepository;

    public WordSetService(WordSetRepository wordSetRepository) {
        this.wordSetRepository = wordSetRepository;
    }

    @Transactional
    public WordSetResponse create(CreateWordSetRequest request) {
        WordSetEntity saved = wordSetRepository.save(WordSetEntity.create(
                request.name().trim(),
                request.description()
        ));
        return WordSetResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<WordSetResponse> list(int page, int pageSize) {
        return PageResponse.from(
                wordSetRepository.findAll(
                                PageRequest.of(Math.max(page - 1, 0), pageSize, Sort.by(Sort.Direction.ASC, "id")))
                        .map(WordSetResponse::from)
        );
    }
}
