import en from './en';

const translations = { en } as const;
type Language = keyof typeof translations;

const currentLanguage: Language = 'en';

export function t(key: keyof typeof en): string {
  return translations[currentLanguage][key];
}

export { en };
export type { Language };
