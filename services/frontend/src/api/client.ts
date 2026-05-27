import createClient from "openapi-fetch";
import type { paths } from "./schema";

const BASE_URL = (import.meta.env.VITE_API_URL as string | undefined) ?? "/api/v1";

export const apiClient = createClient<paths>({ baseUrl: BASE_URL });

export type { paths };
