package com.jp.vocab.wordset.service;

import com.jp.vocab.shared.auth.ContentScope;
import com.jp.vocab.shared.auth.CurrentUserService;
import com.jp.vocab.shared.exception.BusinessException;
import com.jp.vocab.shared.exception.ErrorCode;
import com.jp.vocab.wordset.entity.WordSetEntity;
import com.jp.vocab.wordset.repository.WordSetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WordSetAccessService {

    private final WordSetRepository wordSetRepository;
    private final CurrentUserService currentUserService;

    public WordSetAccessService(WordSetRepository wordSetRepository, CurrentUserService currentUserService) {
        this.wordSetRepository = wordSetRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public WordSetEntity getAccessibleWordSet(Long wordSetId) {
        return wordSetRepository.findAccessibleById(wordSetId, ContentScope.SYSTEM, currentUserService.getCurrentUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Word set not found: " + wordSetId));
    }

    @Transactional(readOnly = true)
    public WordSetEntity getEditableWordSet(Long wordSetId) {
        WordSetEntity wordSet = getAccessibleWordSet(wordSetId);
        if (!ContentScope.USER.equals(wordSet.getScope())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "System word sets are read-only");
        }
        return wordSet;
    }
}
