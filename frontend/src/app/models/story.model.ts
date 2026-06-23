export interface Story {
  id: string;
  repoId: string;
  title: string;
  content: string;
  mode: string;
  startSha: string | null;
  endSha: string | null;
  createdAt: string;
}

export interface CreateStoryRequest {
  mode: string;
  title?: string;
  startSha?: string;
  endSha?: string;
}
