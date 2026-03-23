import { useLanguageStore, LANG_META, ACTIVE_LANGS, type AppLanguage } from "@/store/languageStore";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Button } from "@/components/ui/button";
import { Globe } from "lucide-react";
import { cn } from "@/lib/utils";

interface Props {
  /** "icon" = just globe icon (compact, for sidebar footer)
   *  "full" = globe + native name + chevron (for settings / profile page) */
  variant?: "icon" | "full";
  className?: string;
}

export function LanguageSwitcher({ variant = "icon", className }: Props) {
  const { lang, setLang } = useLanguageStore();
  const current = LANG_META[lang];

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          variant="ghost"
          size={variant === "icon" ? "icon" : "sm"}
          className={cn(
            "text-muted-foreground hover:text-foreground",
            variant === "full" && "gap-2 px-3",
            className
          )}
          title="Change language"
        >
          <Globe className="h-4 w-4 shrink-0" />
          {variant === "full" && (
            <span className="text-sm font-medium">{current.nativeName}</span>
          )}
        </Button>
      </DropdownMenuTrigger>

      <DropdownMenuContent align="end" className="w-44">
        {ACTIVE_LANGS.map((code) => {
          const meta = LANG_META[code];
          return (
            <DropdownMenuItem
              key={code}
              onClick={() => setLang(code as AppLanguage)}
              className={cn(
                "flex items-center justify-between cursor-pointer",
                lang === code && "bg-accent text-accent-foreground font-medium"
              )}
            >
              <span>{meta.nativeName}</span>
              <span className="text-xs text-muted-foreground">{meta.name}</span>
            </DropdownMenuItem>
          );
        })}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
