import { getJson } from "@/shared/api/http";

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
