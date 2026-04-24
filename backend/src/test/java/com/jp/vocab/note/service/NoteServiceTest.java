package com.jp.vocab.note.service;

import com.jp.vocab.note.dto.ImportNotesRequest;
import com.jp.vocab.note.dto.ImportNotesRequestItem;
import com.jp.vocab.note.dto.NoteImportPreviewResponse;
import com.jp.vocab.note.dto.NoteImportResponse;
import com.jp.vocab.note.entity.NoteEntity;
import com.jp.vocab.note.repository.NoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock
    private NoteRepository noteRepository;

    private NoteService noteService;

    @BeforeEach
    void setUp() {
        noteService = new NoteService(
                noteRepository,
                new NoteMarkdownParser(),
                new NoteFsrsScheduler()
        );
    }

    @Test
    void shouldPreviewMarkdownImportWithNormalizedCommonTags() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "notes.md",
                "text/markdown",
                """
                # Java
                ## Stream
                map / filter
                """.getBytes()
        );

        NoteImportPreviewResponse response = noteService.previewImport(file, "H1_H2", "java, stream, java");

        assertEquals("H1_H2", response.splitMode());
        assertEquals(1, response.totalItems());
        assertEquals(1, response.readyCount());
        assertEquals(0, response.errorCount());
        assertEquals("Java / Stream", response.previewItems().getFirst().title());
        assertIterableEquals(List.of("java", "stream"), response.previewItems().getFirst().tags());
    }

    @Test
    void shouldSkipInvalidItemsAndContinueImportingRemainingNotes() {
        when(noteRepository.save(any(NoteEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NoteImportResponse response = noteService.importNotes(new ImportNotesRequest(List.of(
                new ImportNotesRequestItem("  Java Stream  ", "  map / filter  ", List.of("java", "java", " ")),
                new ImportNotesRequestItem("   ", "still invalid", List.of("broken"))
        )));

        assertEquals(1, response.importedCount());
        assertEquals(1, response.skippedCount());
        assertEquals(1, response.errors().size());
        assertEquals("title must not be blank", response.errors().getFirst().message());

        ArgumentCaptor<NoteEntity> savedCaptor = ArgumentCaptor.forClass(NoteEntity.class);
        verify(noteRepository, times(1)).save(savedCaptor.capture());
        assertEquals("Java Stream", savedCaptor.getValue().getTitle());
        assertEquals("map / filter", savedCaptor.getValue().getContent());
        assertIterableEquals(List.of("java"), savedCaptor.getValue().getTags());
    }
}
