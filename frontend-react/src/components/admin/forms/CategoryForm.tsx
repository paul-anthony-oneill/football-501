"use client";

import { useEffect, useState } from "react";
import TextField from "@/components/ui/TextField";
import TextArea from "@/components/ui/TextArea";
import type { Category, CreateCategoryRequest, UpdateCategoryRequest } from "@/lib/types/admin";

interface CategoryFormProps {
  category?: Category;
  loading?: boolean;
  onSubmit: (data: CreateCategoryRequest | UpdateCategoryRequest) => void;
  onCancel: () => void;
}

export default function CategoryForm({
  category,
  loading = false,
  onSubmit,
  onCancel,
}: CategoryFormProps) {
  const isEdit = !!category;

  const [name, setName] = useState(category?.name ?? "");
  const [slug, setSlug] = useState(category?.slug ?? "");
  const [description, setDescription] = useState(category?.description ?? "");
  const [errors, setErrors] = useState<Record<string, string>>({});

  // Auto-generate slug from name when creating
  function handleNameChange(val: string) {
    setName(val);
    if (!isEdit) {
      setSlug(
        val
          .toLowerCase()
          .replace(/[^a-z0-9]+/g, "-")
          .replace(/^-|-$/g, "")
      );
    }
  }

  function validate(): boolean {
    const errs: Record<string, string> = {};
    if (!name.trim()) errs.name = "Name is required";
    if (!isEdit && !slug.trim()) errs.slug = "Slug is required";
    if (!isEdit && slug && !/^[a-z0-9-]+$/.test(slug)) {
      errs.slug = "Slug must contain only lowercase letters, numbers, and hyphens";
    }
    setErrors(errs);
    return Object.keys(errs).length === 0;
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!validate()) return;

    if (isEdit) {
      onSubmit({ name, description } as UpdateCategoryRequest);
    } else {
      onSubmit({ name, slug, description } as CreateCategoryRequest);
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      <TextField
        label="Name"
        value={name}
        onChange={handleNameChange}
        required
        error={errors.name}
        disabled={loading}
      />

      <TextField
        label="Slug"
        value={slug}
        onChange={setSlug}
        required={!isEdit}
        disabled={isEdit || loading}
        error={errors.slug}
        placeholder="e.g. premier-league"
      />

      <TextArea
        label="Description"
        value={description}
        onChange={setDescription}
        disabled={loading}
        rows={3}
      />

      <div className="flex justify-end gap-4 mt-8 pt-4 border-t border-white/10">
        <button
          type="button"
          onClick={onCancel}
          disabled={loading}
          className="px-6 py-3 rounded-lg bg-[#444] text-white font-medium text-base disabled:opacity-50 disabled:cursor-not-allowed hover:opacity-90 transition-opacity"
        >
          Cancel
        </button>
        <button
          type="submit"
          disabled={loading}
          className="px-6 py-3 rounded-lg bg-[var(--color-primary)] text-black font-medium text-base disabled:opacity-50 disabled:cursor-not-allowed hover:opacity-90 transition-opacity"
        >
          {loading ? "Saving…" : isEdit ? "Update Category" : "Create Category"}
        </button>
      </div>
    </form>
  );
}
