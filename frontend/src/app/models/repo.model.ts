export interface GitRepo {
  id: string;
  name: string;
  localPath: string | null;
  remoteUrl: string | null;
  provider: string;
  createdAt: string;
}

export interface CreateRepoRequest {
  name: string;
  localPath?: string;
  remoteUrl?: string;
  provider?: string;
}
