import { getJson, postJson } from "@/shared/api/http";

export interface AnkiTemplate {
  id: number;
  name: string;
  description?: string;
  fieldMapping: Record<string, string[]>;
  frontTemplate: string;
  backTemplate: string;
  cssTemplate?: string;
  createdAt: string;
  updatedAt: string;
}

export interface MarkdownTemplate {
  id: number;
  name: string;
  description?: string;
  templateContent: string;
  createdAt: string;
  updatedAt: string;
}

export function listAnkiTemplates() {
  return getJson<AnkiTemplate[]>("/templates/anki");
}

export function listMarkdownTemplates() {
  return getJson<MarkdownTemplate[]>("/templates/md");
}

export interface TemplateCardSample {
  expression: string;
  reading: string;
  meaning: string;
  partOfSpeech: string;
  exampleJp: string;
  exampleZh: string;
  tags: string[];
  dueDate: string;
  planName: string;
}

export interface AnkiTemplatePreviewPayload {
  frontTemplate: string;
  backTemplate: string;
  cssTemplate?: string;
  sample: TemplateCardSample;
}

export interface AnkiTemplatePreviewResult {
  frontRendered: string;
  backRendered: string;
  cssRendered: string;
}

export interface MarkdownTemplatePreviewPayload {
  templateContent: string;
  date: string;
  planName: string;
  newCards: TemplateCardSample[];
  reviewCards: TemplateCardSample[];
}

export interface MarkdownTemplatePreviewResult {
  renderedContent: string;
}

export function previewAnkiTemplate(payload: AnkiTemplatePreviewPayload) {
  return postJson<AnkiTemplatePreviewResult, AnkiTemplatePreviewPayload>("/templates/anki/preview", payload);
}

export function previewMarkdownTemplate(payload: MarkdownTemplatePreviewPayload) {
  return postJson<MarkdownTemplatePreviewResult, MarkdownTemplatePreviewPayload>("/templates/md/preview", payload);
}
