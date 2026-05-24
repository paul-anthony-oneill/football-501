"use client";

import { useCallback, useEffect, useState } from "react";
import { adminApi } from "@/lib/api/admin";
import type { Category, CreateCategoryRequest, UpdateCategoryRequest } from "@/lib/types/admin";
import DataTable from "@/components/admin/DataTable";
import CategoryForm from "@/components/admin/forms/CategoryForm";
import ConfirmDialog from "@/components/ui/ConfirmDialog";
import FormModal from "@/components/ui/FormModal";
import { useToast } from "@/context/ToastContext";

const columns = [
  { key: "name", label: "Name" },
  { key: "slug", label: "Slug" },
  { key: "questionCount", label: "Questions" },
  { key: "updatedAt", label: "Last Updated" },
];

export default function CategoriesPage() {
  const { addToast } = useToast();

  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);

  // Modal state
  const [showCreate, setShowCreate] = useState(false);
  const [showEdit, setShowEdit] = useState(false);
  const [showDelete, setShowDelete] = useState(false);
  const [selected, setSelected] = useState<Category | null>(null);

  const loadCategories = useCallback(async () => {
    setLoading(true);
    try {
      const data = await adminApi.listCategories();
      setCategories(data);
    } catch (err) {
      addToast((err as Error).message, "error");
    } finally {
      setLoading(false);
    }
  }, [addToast]);

  useEffect(() => {
    loadCategories();
  }, [loadCategories]);

  function handleEdit(category: Category) {
    setSelected(category);
    setShowEdit(true);
  }

  function handleDeleteClick(category: Category) {
    if (category.questionCount > 0) {
      addToast(
        `Cannot delete category with ${category.questionCount} questions`,
        "error"
      );
      return;
    }
    setSelected(category);
    setShowDelete(true);
  }

  async function handleCreate(data: CreateCategoryRequest | UpdateCategoryRequest) {
    setActionLoading(true);
    try {
      await adminApi.createCategory(data as CreateCategoryRequest);
      addToast("Category created successfully", "success");
      setShowCreate(false);
      loadCategories();
    } catch (err) {
      addToast((err as Error).message, "error");
    } finally {
      setActionLoading(false);
    }
  }

  async function handleUpdate(data: CreateCategoryRequest | UpdateCategoryRequest) {
    if (!selected) return;
    setActionLoading(true);
    try {
      await adminApi.updateCategory(selected.id, data as UpdateCategoryRequest);
      addToast("Category updated successfully", "success");
      setShowEdit(false);
      loadCategories();
    } catch (err) {
      addToast((err as Error).message, "error");
    } finally {
      setActionLoading(false);
    }
  }

  async function handleDelete() {
    if (!selected) return;
    setActionLoading(true);
    try {
      await adminApi.deleteCategory(selected.id);
      addToast("Category deleted successfully", "success");
      setShowDelete(false);
      loadCategories();
    } catch (err) {
      addToast((err as Error).message, "error");
    } finally {
      setActionLoading(false);
    }
  }

  return (
    <div>
      {/* Page header */}
      <div className="flex justify-between items-center mb-8">
        <h1 className="m-0 text-[var(--color-primary)]">Categories</h1>
        <button
          onClick={() => setShowCreate(true)}
          className="bg-[var(--color-primary)] text-black px-6 py-3 rounded-lg font-semibold hover:bg-[#22c55e] transition-colors"
        >
          + New Category
        </button>
      </div>

      <DataTable
        columns={columns}
        data={categories}
        loading={loading}
        totalElements={categories.length}
        pageSize={100}
        currentPage={0}
        renderActions={(item) => (
          <>
            <button
              onClick={() => handleEdit(item)}
              title="Edit"
              className="w-10 h-10 flex items-center justify-center bg-[var(--color-surface-variant)] rounded-[var(--radius-sm)] hover:bg-[var(--color-primary-container)] transition-colors"
            >
              ✏️
            </button>
            <button
              onClick={() => handleDeleteClick(item)}
              title="Delete"
              className="w-10 h-10 flex items-center justify-center bg-[var(--color-surface-variant)] rounded-[var(--radius-sm)] hover:bg-[var(--color-error-container)] transition-colors"
            >
              🗑️
            </button>
          </>
        )}
      />

      {/* Create modal */}
      <FormModal
        open={showCreate}
        title="Create Category"
        onSave={() => {}} // form handles its own submit
        onCancel={() => setShowCreate(false)}
        loading={actionLoading}
      >
        <CategoryForm
          loading={actionLoading}
          onSubmit={handleCreate}
          onCancel={() => setShowCreate(false)}
        />
      </FormModal>

      {/* Edit modal */}
      <FormModal
        open={showEdit && !!selected}
        title="Edit Category"
        onSave={() => {}}
        onCancel={() => setShowEdit(false)}
        loading={actionLoading}
      >
        {selected && (
          <CategoryForm
            category={selected}
            loading={actionLoading}
            onSubmit={handleUpdate}
            onCancel={() => setShowEdit(false)}
          />
        )}
      </FormModal>

      {/* Delete confirm */}
      <ConfirmDialog
        open={showDelete}
        title="Delete Category"
        message={`Are you sure you want to delete '${selected?.name}'? This action cannot be undone.`}
        onConfirm={handleDelete}
        onCancel={() => setShowDelete(false)}
      />
    </div>
  );
}
