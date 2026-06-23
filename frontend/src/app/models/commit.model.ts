export interface CommitEntry {
  id: string;
  sha: string;
  authorName: string;
  authorEmail: string;
  authoredAt: string;
  message: string;
  filesChanged: string[];
  additions: number;
  deletions: number;
}
