export interface AnalyticsSummary {
  totalCommits: number;
  totalAuthors: number;
  firstCommit: string | null;
  lastCommit: string | null;
  totalFilesChanged: number;
  topAuthors: { name: string; email: string; commitCount: number }[];
  topFiles: { path: string; changeCount: number }[];
}

export interface TimelinePoint {
  date: string;
  commitCount: number;
}

export interface ActivityDistribution {
  hour: number;
  commitCount: number;
}
