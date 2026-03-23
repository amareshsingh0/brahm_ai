import { useState } from "react";
import { useSearch } from "@/hooks/useSearch";
import { motion } from "framer-motion";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Progress } from "@/components/ui/progress";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  Upload,
  Database,
  FileText,
  HardDrive,
  Search,
  Trash2,
  RefreshCw,
  CheckCircle2,
  Clock,
  AlertCircle,
  BookOpen,
  Languages,
  Cpu,
} from "lucide-react";
import { useTranslation } from "react-i18next";

interface BookEntry {
  id: number;
  name: string;
  format: string;
  size: string;
  language: string;
  chunks: number;
  status: "indexed" | "processing" | "pending";
  addedDate: string;
}

const sampleBooks: BookEntry[] = [
  { id: 1, name: "Bhagavad Gita - Complete Sanskrit-Hindi", format: "PDF", size: "4.2 MB", language: "Sanskrit/Hindi", chunks: 847, status: "indexed", addedDate: "2025-12-01" },
  { id: 2, name: "Rig Veda - Mandala 1-10", format: "TXT", size: "12.8 MB", language: "Sanskrit", chunks: 3241, status: "indexed", addedDate: "2025-12-05" },
  { id: 3, name: "Brihat Parashara Hora Shastra", format: "PDF", size: "8.1 MB", language: "Sanskrit/English", chunks: 2156, status: "indexed", addedDate: "2025-12-10" },
  { id: 4, name: "Yoga Sutras of Patanjali - Commentary", format: "EPUB", size: "2.3 MB", language: "English", chunks: 412, status: "indexed", addedDate: "2025-12-12" },
  { id: 5, name: "Upanishads Collection (108)", format: "PDF", size: "24.5 MB", language: "Sanskrit/English", chunks: 6893, status: "indexed", addedDate: "2025-12-15" },
  { id: 6, name: "Mahabharata - Critical Edition", format: "TXT", size: "48.2 MB", language: "Sanskrit", chunks: 12450, status: "indexed", addedDate: "2025-12-20" },
  { id: 7, name: "Jataka Parijata", format: "PDF", size: "5.7 MB", language: "Sanskrit/Hindi", chunks: 1678, status: "indexed", addedDate: "2026-01-05" },
  { id: 8, name: "Phaladeepika - Mantreshwara", format: "PDF", size: "3.4 MB", language: "Sanskrit/English", chunks: 934, status: "indexed", addedDate: "2026-01-10" },
  { id: 9, name: "Surya Siddhanta", format: "PDF", size: "2.8 MB", language: "Sanskrit", chunks: 567, status: "processing", addedDate: "2026-03-05" },
  { id: 10, name: "Muhurta Chintamani", format: "PDF", size: "1.9 MB", language: "Sanskrit/Hindi", chunks: 0, status: "pending", addedDate: "2026-03-06" },
];

const systemStats = {
  totalBooks: 1247,
  totalChunks: 312450,
  indexSize: "2.4 GB",
  embeddingModel: "MiniLM-L12-v2",
  vectorDB: "FAISS (HNSW)",
  languages: ["Sanskrit", "Hindi", "English"],
  lastUpdated: "2026-03-06",
  searchLatency: "~25ms",
};

const statusColors = {
  indexed: "bg-green-500/10 text-green-400 border-green-500/30",
  processing: "bg-yellow-500/10 text-yellow-400 border-yellow-500/30",
  pending: "bg-muted/30 text-muted-foreground border-border/30",
};

export default function KnowledgeBasePage() {
  const { t } = useTranslation();
  const [search, setSearch] = useState("");
  const [dragOver, setDragOver] = useState(false);
  const { data: ragResults } = useSearch(search);

  const statusIcons = {
    indexed: <CheckCircle2 className="h-3.5 w-3.5 text-green-500" />,
    processing: <RefreshCw className="h-3.5 w-3.5 text-yellow-500 animate-spin" />,
    pending: <Clock className="h-3.5 w-3.5 text-muted-foreground" />,
  };

  const filtered = sampleBooks.filter(
    (b) => !search || b.name.toLowerCase().includes(search.toLowerCase()) || b.language.toLowerCase().includes(search.toLowerCase()),
  );

  const indexedCount = sampleBooks.filter((b) => b.status === "indexed").length;

  return (
    <div className="space-y-6">
      <motion.div initial={{ opacity: 0, y: -20 }} animate={{ opacity: 1, y: 0 }}>
        <h1 className="font-display text-3xl text-primary text-glow-gold">{t("knowledge.title")}</h1>
        <p className="text-muted-foreground mt-1">
          {t("knowledge.subtitle")}
        </p>
      </motion.div>

      <Tabs defaultValue="library">
        <TabsList>
          <TabsTrigger value="library" className="gap-1.5">
            <BookOpen className="h-3.5 w-3.5" />
            {t("knowledge.tab_library")}
          </TabsTrigger>
          <TabsTrigger value="upload" className="gap-1.5">
            <Upload className="h-3.5 w-3.5" />
            {t("knowledge.tab_upload")}
          </TabsTrigger>
          <TabsTrigger value="system" className="gap-1.5">
            <Database className="h-3.5 w-3.5" />
            {t("knowledge.tab_system")}
          </TabsTrigger>
        </TabsList>

        {/* Library Tab */}
        <TabsContent value="library" className="space-y-4 mt-4">
          <div className="flex items-center gap-3">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder={t("knowledge.search_placeholder")}
                className="pl-9 bg-muted/30 border-border/40"
              />
            </div>
            <Badge variant="secondary">{indexedCount}/{sampleBooks.length} {t("knowledge.indexed")}</Badge>
          </div>

          <div className="space-y-2">
            {filtered.map((book, i) => (
              <motion.div key={book.id} initial={{ opacity: 0, x: -10 }} animate={{ opacity: 1, x: 0 }} transition={{ delay: i * 0.03 }}>
                <Card className="glass border-border/30">
                  <CardContent className="p-3 flex items-center gap-3">
                    <div className="p-2 rounded-lg bg-muted/30">
                      <FileText className="h-4 w-4 text-primary" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium truncate">{book.name}</p>
                      <div className="flex items-center gap-2 mt-0.5">
                        <span className="text-xs text-muted-foreground">{book.format}</span>
                        <span className="text-xs text-muted-foreground">{book.size}</span>
                        <Badge variant="outline" className="text-xs h-4">{book.language}</Badge>
                      </div>
                    </div>
                    <div className="flex items-center gap-3 shrink-0">
                      {book.chunks > 0 && (
                        <span className="text-xs text-muted-foreground">{book.chunks.toLocaleString()} {t("knowledge.chunks")}</span>
                      )}
                      <Badge variant="outline" className={`text-xs gap-1 ${statusColors[book.status]}`}>
                        {statusIcons[book.status]}
                        {t(`knowledge.status_${book.status}`)}
                      </Badge>
                      <Button variant="ghost" size="icon" className="h-7 w-7">
                        <Trash2 className="h-3 w-3 text-muted-foreground" />
                      </Button>
                    </div>
                  </CardContent>
                </Card>
              </motion.div>
            ))}
          </div>
        </TabsContent>

        {/* Upload Tab */}
        <TabsContent value="upload" className="space-y-4 mt-4">
          <Card
            className={`glass border-2 border-dashed transition-colors ${dragOver ? "border-primary bg-primary/5" : "border-border/40"}`}
            onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
            onDragLeave={() => setDragOver(false)}
            onDrop={(e) => { e.preventDefault(); setDragOver(false); }}
          >
            <CardContent className="p-12 text-center">
              <Upload className={`h-12 w-12 mx-auto mb-4 ${dragOver ? "text-primary" : "text-muted-foreground"}`} />
              <h3 className="font-display text-lg mb-1">{t("knowledge.upload_title")}</h3>
              <p className="text-sm text-muted-foreground mb-4">
                {t("knowledge.upload_desc")}
              </p>
              <div className="flex flex-wrap justify-center gap-2 mb-4">
                {["PDF", "TXT", "JSON", "JSONL", "CSV", "EPUB", "MD", "HTML"].map((f) => (
                  <Badge key={f} variant="outline" className="text-xs">{f}</Badge>
                ))}
              </div>
              <Button variant="outline">
                <Upload className="h-4 w-4 mr-2" />
                {t("knowledge.choose_files")}
              </Button>
            </CardContent>
          </Card>

          <Card className="glass border-border/30">
            <CardHeader className="pb-2">
              <CardTitle className="text-base">{t("knowledge.pipeline_title")}</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              {[
                { step: t("knowledge.pipeline_step1"), desc: t("knowledge.pipeline_step1_desc"), progress: 100 },
                { step: t("knowledge.pipeline_step2"), desc: t("knowledge.pipeline_step2_desc"), progress: 100 },
                { step: t("knowledge.pipeline_step3"), desc: t("knowledge.pipeline_step3_desc"), progress: 75 },
                { step: t("knowledge.pipeline_step4"), desc: t("knowledge.pipeline_step4_desc"), progress: 50 },
                { step: t("knowledge.pipeline_step5"), desc: t("knowledge.pipeline_step5_desc"), progress: 0 },
              ].map((s) => (
                <div key={s.step}>
                  <div className="flex items-center justify-between mb-1">
                    <p className="text-xs font-medium">{s.step}</p>
                    <span className="text-xs text-muted-foreground">{s.progress}%</span>
                  </div>
                  <p className="text-xs text-muted-foreground mb-1.5">{s.desc}</p>
                  <Progress value={s.progress} className="h-1.5" />
                </div>
              ))}
            </CardContent>
          </Card>
        </TabsContent>

        {/* System Tab */}
        <TabsContent value="system" className="space-y-4 mt-4">
          <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-3">
            {[
              { icon: BookOpen, label: t("knowledge.stat_total_books"), value: systemStats.totalBooks.toLocaleString() },
              { icon: Database, label: t("knowledge.stat_total_chunks"), value: systemStats.totalChunks.toLocaleString() },
              { icon: HardDrive, label: t("knowledge.stat_index_size"), value: systemStats.indexSize },
              { icon: Cpu, label: t("knowledge.stat_search_latency"), value: systemStats.searchLatency },
            ].map((stat) => (
              <Card key={stat.label} className="glass border-border/30">
                <CardContent className="p-4 flex items-center gap-3">
                  <div className="p-2 rounded-lg bg-primary/10">
                    <stat.icon className="h-5 w-5 text-primary" />
                  </div>
                  <div>
                    <p className="text-xs text-muted-foreground">{stat.label}</p>
                    <p className="text-lg font-display">{stat.value}</p>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>

          <Card className="glass border-border/30">
            <CardHeader className="pb-2">
              <CardTitle className="text-base flex items-center gap-2">
                <Database className="h-4 w-4 text-primary" />
                {t("knowledge.system_config")}
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid sm:grid-cols-2 gap-3">
                {[
                  { label: t("knowledge.cfg_embedding_model"), value: systemStats.embeddingModel },
                  { label: t("knowledge.cfg_vector_db"), value: systemStats.vectorDB },
                  { label: t("knowledge.cfg_llm"), value: "Qwen 2.5-7B-Instruct (4-bit)" },
                  { label: t("knowledge.cfg_search"), value: "Hybrid (FAISS + BM25 + Rerank)" },
                  { label: t("knowledge.cfg_cache"), value: "Redis Semantic Cache" },
                  { label: t("knowledge.cfg_gpu"), value: "NVIDIA L4 24GB VRAM" },
                  { label: t("knowledge.cfg_platform"), value: "Google Cloud VM" },
                  { label: t("knowledge.cfg_last_updated"), value: systemStats.lastUpdated },
                ].map((item) => (
                  <div key={item.label} className="flex justify-between bg-muted/20 rounded-lg p-3">
                    <span className="text-xs text-muted-foreground">{item.label}</span>
                    <span className="text-xs font-medium">{item.value}</span>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>

          <Card className="glass border-border/30">
            <CardHeader className="pb-2">
              <CardTitle className="text-base flex items-center gap-2">
                <Languages className="h-4 w-4 text-primary" />
                {t("knowledge.language_support")}
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-3 gap-3">
                {[
                  { lang: "Sanskrit", script: "संस्कृतम्", books: "45,000+", status: t("knowledge.lang_status_primary") },
                  { lang: "Hindi", script: "हिन्दी", books: "32,000+", status: t("knowledge.lang_status_primary") },
                  { lang: "English", script: "English", books: "23,000+", status: t("knowledge.lang_status_primary") },
                ].map((l) => (
                  <div key={l.lang} className="bg-primary/5 border border-primary/20 rounded-lg p-3 text-center">
                    <p className="text-lg font-serif">{l.script}</p>
                    <p className="text-xs font-medium mt-1">{l.lang}</p>
                    <p className="text-xs text-muted-foreground">{l.books} {t("knowledge.texts")}</p>
                    <Badge variant="secondary" className="text-xs mt-1">{l.status}</Badge>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>

          <Card className="glass border-border/30">
            <CardContent className="p-4">
              <div className="bg-yellow-500/10 border border-yellow-500/20 rounded-lg p-4 flex items-start gap-3">
                <AlertCircle className="h-5 w-5 text-yellow-500 shrink-0 mt-0.5" />
                <div>
                  <p className="text-sm font-medium text-yellow-400">{t("knowledge.backend_title")}</p>
                  <p className="text-xs text-muted-foreground mt-0.5">
                    {t("knowledge.backend_desc")}
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
}
