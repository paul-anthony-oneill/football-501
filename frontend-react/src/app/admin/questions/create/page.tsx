"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { adminApi } from "@/lib/api/admin";
import type { Category, CreateQuestionRequest, UpdateQuestionRequest } from "@/lib/types/admin";
import QuestionForm from "@/components/admin/forms/QuestionForm";
import { useToast } from "@/context/ToastContext";

export default function CreateQuestionPage() {
  const { addToast } = useToast();
  const router = useRouter();

  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    adminApi.listCategories().then(setCategories).catch((err) => {
      addToast((err as Error).message, "error");
    });
  }, [addToast]);

  async function handleSubmit(
    data: CreateQuestionRequest | UpdateQuestionRequest
  ) {
    setLoading(true);
    try {
      const question = await adminApi.createQuestion(data as CreateQuestionRequest);
      addToast("Question created successfully", "success");
      // Navigate to the edit page so answers can be added immediately
      router.push(`/admin/questions/${question.id}`);
    } catch (err) {
      addToast((err as Error).message, "error");
      setLoading(false);
    }
  }

  return (
    <div className="max-w-3xl mx-auto">
      <h1 className="text-[var(--color-primary)] mb-8">Create Question</h1>

      <div className="bg-[#2a2a2a] p-8 rounded-xl">
        <QuestionForm
          categories={categories}
          loading={loading}
          onSubmit={handleSubmit}
          onCancel={() => router.push("/admin/questions")}
        />
      </div>
    </div>
  );
}
