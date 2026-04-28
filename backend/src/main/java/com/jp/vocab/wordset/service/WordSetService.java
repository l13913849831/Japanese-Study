package com.jp.vocab.wordset.service;

import com.jp.vocab.shared.api.PageResponse;
import com.jp.vocab.shared.auth.ContentScope;
import com.jp.vocab.shared.auth.CurrentUserService;
import com.jp.vocab.shared.exception.BusinessException;
import com.jp.vocab.shared.exception.ErrorCode;
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
    private final CurrentUserService currentUserService;

    public WordSetService(WordSetRepository wordSetRepository, CurrentUserService currentUserService) {
        this.wordSetRepository = wordSetRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public WordSetResponse create(CreateWordSetRequest request) {
        Long userId = currentUserService.getCurrentUserId();
        String name = request.name().trim();
        if (wordSetRepository.existsByOwnerUserIdAndName(userId, name)) {
            throw new BusinessException(ErrorCode.CONFLICT, "Word set name already exists: " + name);
        }

        WordSetEntity saved = wordSetRepository.save(WordSetEntity.createUserOwned(name, request.description(), userId));
        return WordSetResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<WordSetResponse> list(int page, int pageSize) {
        Long userId = currentUserService.getCurrentUserId();
        return PageResponse.from(
                wordSetRepository.findAccessible(
                                ContentScope.SYSTEM,
                                userId,
                                PageRequest.of(Math.max(page - 1, 0), pageSize, Sort.by(Sort.Direction.ASC, "id")))
                        .map(WordSetResponse::from)
        );
    }
}
