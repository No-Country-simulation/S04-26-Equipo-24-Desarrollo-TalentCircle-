export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: string;
  user: UserDto;
}

export interface UserDto {
  id: string;
  email: string;
  fullName: string;
  role: 'ADMIN' | 'EDITOR';
  active: boolean;
}

export interface DraftSummaryDto {
  id: string;
  channel: 'NEWSLETTER' | 'LINKEDIN' | 'TWITTER';
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'PUBLISHED';
  createdAt: string;
  summary: string;
}

export interface DraftDetailDto {
  id: string;
  channel: 'NEWSLETTER' | 'LINKEDIN' | 'TWITTER';
  content: string;
  editedContent: string | null;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'PUBLISHED';
  aiScore: number;
  createdAt: string;
  updatedAt: string;
  sources: DraftSourceDto[];
  versions: DraftVersionDto[];
}

export interface DraftSourceDto {
  id: string;
  title: string;
  relevanceScore: number;
}

export interface DraftVersionDto {
  id: string;
  content: string;
  editedBy: string | null;
  editedAt: string;
  versionNumber: number;
}

export interface SourceDto {
  id: string;
  name: string;
  type: 'DISCORD' | 'CIRCLE' | 'SLACK';
  active: boolean;
}

export interface ConfigDto {
  llmProvider: string;
  llmModel: string;
  newsletterPrompt: string;
  linkedinPrompt: string;
  twitterPrompt: string;
  maxItemsPerChannel: number;
  scheduleCron: string;
}

export interface ExecutionSummaryDto {
  id: string;
  weekStart: string;
  weekEnd: string;
  status: 'COMPLETED' | 'RUNNING' | 'FAILED';
  startedAt: string;
  completedAt: string | null;
}

export interface CommunityActivityDto {
  id: string;
  title: string;
  content: string;
  type: 'POST' | 'QUESTION' | 'RESOURCE';
  reactionCount: number;
  responseCount: number;
  shareCount: number;
  author: string;
  sourceUrl: string;
}

export interface ApiErrorResponse {
  error: string;
  message: string;
  timestamp: string;
}
