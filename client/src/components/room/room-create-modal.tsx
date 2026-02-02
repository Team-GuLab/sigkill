"use client";

import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/ui/dialog";
import { Button } from "@/ui/button";
import { Input } from "@/ui/input";
import { Field, FieldLabel } from "@/ui/field";
import { useCreateRoom } from "@/hooks/room/use-create-room";
import { useState } from "react";
import { toast } from "sonner";
import { AppError } from "@/api/axios";

interface RoomCreateModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function RoomCreateModal({ open, onOpenChange }: RoomCreateModalProps) {
  const [title, setTitle] = useState("");

  const { mutate: createRoom, isPending: isCreatingRoom } = useCreateRoom({
    onSuccess: () => {
      toast.success("방이 성공적으로 생성되었습니다!");
      onOpenChange(false);
    },
    onError: (error) => {
      toast.error(error.message);
    },
  });

  const handleCreateRoomButtonClick = () => {
    createRoom({ title });
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent showCloseButton={false}>
        <DialogHeader>
          <DialogTitle>방 생성</DialogTitle>
        </DialogHeader>

        <Field>
          <FieldLabel htmlFor="room-title">방 제목</FieldLabel>
          <Input
            id="room-title"
            placeholder="방 제목을 입력하세요"
            required
            onChange={(e) => setTitle(e.target.value)}
          />
        </Field>

        <DialogFooter className="flex w-full flex-row gap-2 sm:flex-row sm:justify-stretch">
          <Button
            variant="outline"
            className="flex-1 bg-transparent"
            onClick={() => onOpenChange(false)}
          >
            닫기
          </Button>
          <Button
            className="flex-1"
            disabled={!title || isCreatingRoom}
            onClick={handleCreateRoomButtonClick}
          >
            생성하기
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
