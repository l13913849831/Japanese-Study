package com.jp.vocab.exportjob.service;

import com.jp.vocab.exportjob.dto.ExportJobResponse;
import com.jp.vocab.exportjob.repository.ExportJobRepository;
import com.jp.vocab.shared.api.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExportJobService {

    private final ExportJobRepository exportJobRepository;

    public ExportJobService(ExportJobRepository exportJobRepository) {
        this.exportJobRepository = exportJobRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ExportJobResponse> list(int page, int pageSize) {
        return PageResponse.from(
                exportJobRepository.findAll(
                                PageRequest.of(Math.max(page - 1, 0), pageSize, Sort.by(Sort.Direction.DESC, "createdAt")))
                        .map(ExportJobResponse::from)
        );
    }
}
