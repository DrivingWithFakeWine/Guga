import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router'
import { login } from '../api/auth'
import { errorMessage } from '../api/client'
import { useAuth } from '../lib/auth'
import { loginSchema, type LoginInput } from '../lib/schemas'
import { Button } from '../components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card'
import { Input } from '../components/ui/input'
import { Label } from '../components/ui/label'

export default function LoginPage() {
  const { setAuth } = useAuth()
  const navigate = useNavigate()
  const form = useForm<LoginInput>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '' },
  })

  const mutation = useMutation({
    mutationFn: (input: LoginInput) => login(input.email, input.password),
    onSuccess: (data) => {
      setAuth(data.token, data.user)
      navigate('/')
    },
  })

  return (
    <Card className="mx-auto mt-10 max-w-sm">
      <CardHeader>
        <CardTitle>登录</CardTitle>
      </CardHeader>
      <CardContent>
        <form onSubmit={form.handleSubmit((input) => mutation.mutate(input))} className="space-y-4">
          <div className="space-y-1.5">
            <Label htmlFor="email">邮箱</Label>
            <Input id="email" type="email" placeholder="you@example.com" {...form.register('email')} />
            {form.formState.errors.email && (
              <p className="text-xs text-destructive">{form.formState.errors.email.message}</p>
            )}
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="password">密码</Label>
            <Input id="password" type="password" placeholder="••••••••" {...form.register('password')} />
            {form.formState.errors.password && (
              <p className="text-xs text-destructive">{form.formState.errors.password.message}</p>
            )}
          </div>
          {mutation.isError && (
            <p className="text-sm text-destructive">{errorMessage(mutation.error, '登录失败，请检查邮箱或密码')}</p>
          )}
          <Button type="submit" className="w-full" disabled={mutation.isPending}>
            {mutation.isPending ? '登录中…' : '登录'}
          </Button>
          <p className="text-center text-sm text-muted-foreground">
            还没有账号？{' '}
            <Link to="/register" className="text-primary hover:underline">
              去注册
            </Link>
          </p>
        </form>
      </CardContent>
    </Card>
  )
}
