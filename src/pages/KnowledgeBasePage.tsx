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

const statusIcons = {
  indexed: <CheckCircle2 className="h-3.5 w-3.5 text-green-500" />,
  processing: <RefreshCw className="h-3.5 w-3.5 text-yellow-500 animate-spin" />,
  pending: <Clock className="h-3.5 w-3.5 text-muted-foreground" />,
};

const statusColors = {
  indexed: "bg-green-500/10 text-green-400 border-green-500/30",
  processing: "bg-yellow-500/10 text-yellow-400 border-yellow-500/30",
  pending: "bg-muted/30 text-muted-foreground border-border/30",
};

export default function KnowledgeBasePage() {
  const [search, setSearch] = useState("");
  const [dragOver, setDragOver] = useState(false);
  const { data: ragResults } = useSearch(search);

  const filtered = sampleBooks.filter(
    (b) => !search || b.name.toLowerCase().includes(search.toLowerCase()) || b.language.toLowerCase().includes(search.toLowerCase()),
  );

  const indexedCount = sampleBooks.filter((b) => b.status === "indexed").length;

  return (
    <div className="space-y-6">
      <motion.div initial={{ opacity: 0, y: -20 }} animate={{ opacity: 1, y: 0 }}>
        <h1 className="font-display text-3xl text-primary text-glow-gold">Knowledge Base</h1>
        <p className="text-muted-foreground mt-1">
          Manage the RAG knowledge base — Upload, index, and search across 100k+ texts
        </p>
      </motion.div>

      <Tabs defaultValue="library">
        <TabsList>
          <TabsTrigger value="library" className="gap-1.5">
            <BookOpen className="h-3.5 w-3.5" />
            Library
          </TabsTrigger>
          <TabsTrigger value="upload" className="gap-1.5">
            <Upload className="h-3.5 w-3.5" />
            Upload
          </TabsTrigger>
          <TabsTrigger value="system" className="gap-1.5">
            <Database className="h-3.5 w-3.5" />
            System
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
                placeholder="Search books..."
                className="pl-9 bg-muted/30 border-border/40"
              />
            </div>
            <Badge variant="secondary">{indexedCount}/{sampleBooks.length} indexed</Badge>
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
                        <span className="text-xs text-muted-foreground">{book.chunks.toLocaleString()} chunks</span>
                      )}
                      <Badge variant="outline" className={`text-xs gap-1 ${statusColors[book.status]}`}>
                        {statusIcons[book.status]}
                        {book.status}
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
              <h3 className="font-display text-lg mb-1">Upload Books & Texts</h3>
              <p className="text-sm text-muted-foreground mb-4">
                Drag & drop files or click to browse
              </p>
              <div className="flex flex-wrap justify-center gap-2 mb-4">
                {["PDF", "TXT", "JSON", "JSONL", "CSV", "EPUB", "MD", "HTML"].map((f) => (
                  <Badge key={f} variant="outline" className="text-xs">{f}</Badge>
                ))}
              </div>
              <Button variant="outline">
                <Upload className="h-4 w-4 mr-2" />
                Choose Files
              </Button>
            </CardContent>
          </Card>

          <Card className="glass border-border/30">
            <CardHeader className="pb-2">
              <CardTitle className="text-base">Processing Pipeline</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              {[
                { step: "1. File Parsing", desc: "Extract text from PDF/EPUB/HTML using pypdf, ebooklib, BeautifulSoup", progress: 100 },
                { step: "2. Text Cleaning", desc: "Remove headers, footers, page numbers. Normalize Unicode for Sanskrit", progress: 100 },
                { step: "3. Chunking", desc: "Split into 400-token chunks with 50-token overlap for context continuity", progress: 75 },
                { step: "4. Embedding", desc: "Generate vectors using MiniLM-L12 multilingual model (384 dim)", progress: 50 },
                { step: "5. FAISS Indexing", desc: "Add to HNSW index with metadata (book, chapter, page, verse)", progress: 0 },
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
              { icon: BookOpen, label: "Total Books", value: systemStats.totalBooks.toLocaleString() },
              { icon: Database, label: "Total Chunks", value: systemStats.totalChunks.toLocaleString() },
              { icon: HardDrive, label: "Index Size", value: systemStats.indexSize },
              { icon: Cpu, label: "Search Latency", value: systemStats.searchLatency },
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
                System Configuration
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid sm:grid-cols-2 gap-3">
                {[
                  { label: "Embedding Model", value: systemStats.embeddingModel },
                  { label: "Vector Database", value: systemStats.vectorDB },
                  { label: "LLM", value: "Qwen 2.5-7B-Instruct (4-bit)" },
                  { label: "Search", value: "Hybrid (FAISS + BM25 + Rerank)" },
                  { label: "Cache", value: "Redis Semantic Cache" },
                  { label: "GPU", value: "NVIDIA L4 24GB VRAM" },
                  { label: "Platform", value: "Google Cloud VM" },
                  { label: "Last Updated", value: systemStats.lastUpdated },
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
                Language Support
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-3 gap-3">
                {[
                  { lang: "Sanskrit", script: "संस्कृतम्", books: "45,000+", status: "Primary" },
                  { lang: "Hindi", script: "हिन्दी", books: "32,000+", status: "Primary" },
                  { lang: "English", script: "English", books: "23,000+", status: "Primary" },
                ].map((l) => (
                  <div key={l.lang} className="bg-primary/5 border border-primary/20 rounded-lg p-3 text-center">
                    <p className="text-lg font-serif">{l.script}</p>
                    <p className="text-xs font-medium mt-1">{l.lang}</p>
                    <p className="text-xs text-muted-foreground">{l.books} texts</p>
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
                  <p className="text-sm font-medium text-yellow-400">Backend Connection</p>
                  <p className="text-xs text-muted-foreground mt-0.5">
                    The RAG backend (FastAPI + Qwen + FAISS) is configured to run on Google Cloud GPU VM.
                    Start the VM and run the API server to enable live AI chat and search functionality.
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
