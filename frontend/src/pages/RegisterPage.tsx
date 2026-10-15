import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router'
import { register } from '../api/auth'
import { errorMessage } from '../api/client'
import { useAuth } from '../lib/auth'
import { registerSchema, type RegisterInput } from '../lib/schemas'
import { Button } from '../components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card'
import { Input } from '../components/ui/input'
import { Label } from '../components/ui/label'

export default function RegisterPage() {
  const { setAuth } = useAuth()
  const navigate = useNavigate()
  const form = useForm<RegisterInput>({
    resolver: zodResolver(registerSchema),
    defaultValues: { username: '', email: '', password: '' },
  })

  const mutation = useMutation({
    mutationFn: (input: RegisterInput) => register(input.username, input.email, input.password),
    onSuccess: (data) => {
      setAuth(data.token, data.user)
      navigate('/')
    },
  })

  return (
    <Card className="mx-auto mt-10 max-w-sm">
      <CardHeader>
        <CardTitle>注册</CardTitle>
      </CardHeader>
      <CardContent>
        <form onSubmit={form.handleSubmit((input) => mutation.mutate(input))} className="space-y-4">
          <div className="space-y-1.5">
            <Label htmlFor="username">用户名</Label>
            <Input id="username" placeholder="3-20 位字母、数字或下划线" {...form.register('username')} />
            {form.formState.errors.username && (
              <p className="text-xs text-destructive">{form.formState.errors.username.message}</p>
            )}
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="email">邮箱</Label>
            <Input id="email" type="email" placeholder="you@example.com" {...form.register('email')} />
            {form.formState.errors.email && (
              <p className="text-xs text-destructive">{form.formState.errors.email.message}</p>
            )}
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="password">密码</Label>
            <Input id="password" type="password" placeholder="至少 8 位" {...form.register('password')} />
            {form.formState.errors.password && (
              <p className="text-xs text-destructive">{form.formState.errors.password.message}</p>
            )}
          </div>
          {mutation.isError && (
            <p className="text-sm text-destructive">{errorMessage(mutation.error, '注册失败，请稍后重试')}</p>
          )}
          <Button type="submit" className="w-full" disabled={mutation.isPending}>
            {mutation.isPending ? '注册中…' : '注册'}
          </Button>
          <p className="text-center text-sm text-muted-foreground">
            已有账号？{' '}
            <Link to="/login" className="text-primary hover:underline">
              去登录
            </Link>
          </p>
        </form>
      </CardContent>
    </Card>
  )
}
